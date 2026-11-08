package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.Mc;
import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import com.workshopcraft.simplegolems.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class Storage {
    public static final String WORK_TAG = "simplegolems_work";
    public static final int NEARBY = 2;
    public static final TagKey<Block> EXTRA_STORAGE = TagKey.create(Registries.BLOCK, SimpleGolems.id("storage"));
    public static final TagKey<Block> NOT_STORAGE = TagKey.create(Registries.BLOCK, SimpleGolems.id("not_storage"));
    // set by each loader: another mod's item storage at a block, through the loader's own item API
    public static BiFunction<ServerLevel, BlockPos, @Nullable Container> external = (level, pos) -> null;

    private Storage() {
    }

    public static boolean isStorage(BlockEntity be) {
        if (be.getBlockState().is(NOT_STORAGE)) return false;
        return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity
                || be.getBlockState().is(EXTRA_STORAGE);
    }

    // vanilla chests, barrels and shulker boxes, blocks in the storage tag, and other mods' blocks with an item inventory
    public static @Nullable Container containerAt(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        if (isStorage(be)) {
            Container c = HopperBlockEntity.getContainerAt(level, pos);
            return c != null ? c : Config.moddedStorage ? external.apply(level, pos) : null;
        }
        if (!Config.moddedStorage || be.getBlockState().is(NOT_STORAGE) || BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getNamespace().equals("minecraft")) return null;
        return external.apply(level, pos);
    }

    // nearest storage in or just around the area to 'from' that matches the test
    public static @Nullable BlockPos nearest(ServerLevel level, WorkArea area, Vec3 from, Predicate<Container> test, Predicate<BlockPos> skip) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int cx = (area.min().getX() - NEARBY) >> 4; cx <= (area.max().getX() + NEARBY) >> 4; cx++) {
            for (int cz = (area.min().getZ() - NEARBY) >> 4; cz <= (area.max().getZ() + NEARBY) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (!area.near(pos, NEARBY) || skip.test(pos)) continue;
                    double d = from.distanceToSqr(Vec3.atCenterOf(pos));
                    if (d >= bestDist) continue;
                    Container c = containerAt(level, pos);
                    if (c != null && test.test(c)) {
                        best = pos.immutable();
                        bestDist = d;
                    }
                }
            }
        }
        return best;
    }

    public static boolean hasRoom(Container c, ItemStack stack) {
        if (c instanceof ExternalStore e) return e.put(stack, true) > 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (!c.canPlaceItem(i, stack)) continue;
            ItemStack in = c.getItem(i);
            if (in.isEmpty()) return true;
            if (Mc.sameItem(in, stack) && in.getCount() < Math.min(in.getMaxStackSize(), maxStack(c, in))) return true;
        }
        return false;
    }

    public static ItemStack insert(Container c, ItemStack stack) {
        if (c instanceof ExternalStore e) {
            if (stack.isEmpty()) return stack;
            int in = e.put(stack, false);
            return stack.copyWithCount(stack.getCount() - in);
        }
        return HopperBlockEntity.addItem(null, c, stack, null);
    }

    // cosmetic open/close only — never call startOpen(player).
    // that API is for real player GUIs; using the owner as a fake opener desyncs vanilla's opener count
    // (recheck closes, then the stale count reopens the lid and it sticks until chunk reload)
    public static void open(ServerLevel level, BlockPos pos, GolemEntity golem) {
        BlockEntity be = level.getBlockEntity(pos);
        clearStaleOpeners(level, pos, be, golem);
        setLid(level, pos, be, true);
        playLid(level, pos, be, true);
    }

    public static void close(ServerLevel level, BlockPos pos, GolemEntity golem) {
        BlockEntity be = level.getBlockEntity(pos);
        clearStaleOpeners(level, pos, be, golem);
        setLid(level, pos, be, false);
        playLid(level, pos, be, false);
    }

    // undo leaked startOpen counts from older builds so the chest's own ticker cannot force the lid back open
    private static void clearStaleOpeners(ServerLevel level, BlockPos pos, @Nullable BlockEntity be, GolemEntity golem) {
        if (be == null) return;
        Player p = golem.getOwner() == null ? null : level.getPlayerByUUID(golem.getOwner());
        if (p == null && !level.players().isEmpty()) p = level.players().get(0);
        if (p == null) return;
        boolean looksOpen = be instanceof LidBlockEntity lid && lid.getOpenNess(1.0F) > 0.01F
                || be instanceof BarrelBlockEntity && level.getBlockState(pos).getValue(BarrelBlock.OPEN);
        if (!looksOpen) return;
        for (int i = 0; i < 4; i++) {
            if (be instanceof ChestBlockEntity c) c.stopOpen(p);
            else if (be instanceof BarrelBlockEntity b) b.stopOpen(p);
            else if (be instanceof ShulkerBoxBlockEntity s) s.stopOpen(p);
            else break;
            if (be instanceof LidBlockEntity lid && lid.getOpenNess(1.0F) <= 0.01F) break;
            if (be instanceof BarrelBlockEntity && !level.getBlockState(pos).getValue(BarrelBlock.OPEN)) break;
        }
    }

    private static void setLid(ServerLevel level, BlockPos pos, @Nullable BlockEntity be, boolean open) {
        BlockState state = level.getBlockState(pos);
        if (be instanceof BarrelBlockEntity && state.hasProperty(BarrelBlock.OPEN)) {
            if (state.getValue(BarrelBlock.OPEN) != open) {
                level.setBlock(pos, state.setValue(BarrelBlock.OPEN, open), Block.UPDATE_CLIENTS);
            }
            return;
        }
        // chests: block event 1 drives the lid pose (same signal openerCountChanged sends)
        if (be instanceof ChestBlockEntity) {
            level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
        }
    }

    private static void playLid(ServerLevel level, BlockPos pos, @Nullable BlockEntity be, boolean open) {
        if (be instanceof BarrelBlockEntity) {
            level.playSound(null, pos, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.5F, 1.0F);
        } else if (be instanceof ShulkerBoxBlockEntity) {
            level.playSound(null, pos, open ? SoundEvents.SHULKER_BOX_OPEN : SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.5F, 1.0F);
        } else {
            level.playSound(null, pos, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
    }

    public static void pickUp(GolemEntity golem, ItemEntity item) {
        ItemStack stack = item.getItem();
        if (!golem.canCarry(stack)) return;
        golem.addCarried(stack);
        if (stack.isEmpty()) item.discard();
        else item.setItem(stack);
        golem.playSound(SoundEvents.ITEM_PICKUP, 0.3F, 1.6F);
    }

    // what fits the carry slots goes straight in, the rest is dropped for later trips
    public static void collectOrDrop(GolemEntity golem, ServerLevel level, Vec3 at, ItemStack stack) {
        golem.addCarried(stack);
        spawnWorkDrop(level, at, stack);
    }

    public static void spawnWorkDrop(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity e = new ItemEntity(level, at.x, at.y, at.z, stack);
        e.setDefaultPickUpDelay();
        e.addTag(WORK_TAG);
        level.addFreshEntity(e);
    }

    private static int maxStack(Container c, ItemStack s) {
        //? if >=1.20.5 {
        return c.getMaxStackSize(s);
        //?} else {
        /*return c.getMaxStackSize();
        *///?}
    }
}
