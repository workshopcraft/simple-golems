package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.Mc;
import com.workshopcraft.simplegolems.registry.ModTags;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import com.workshopcraft.simplegolems.item.GolemProgram;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.Collections;

public class FarmerWork implements JobWork {
    public static final FarmerWork INSTANCE = new FarmerWork();

    @Override
    public boolean wants(ItemEntity item) {
        return Mc.tags(item).contains(Storage.WORK_TAG);
    }

    // CropBlock covers vanilla and most modded crops; anything else in #crops with a plain age property also counts
    static @Nullable IntegerProperty age(BlockState state) {
        if (state.getBlock() instanceof StemBlock || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) return null;
        if (!(state.getBlock() instanceof CropBlock) && !state.is(ModTags.CROPS)) return null;
        for (Property<?> p : state.getProperties()) {
            if (p instanceof IntegerProperty ip && p.getName().equals("age")) return ip;
        }
        return null;
    }

    static boolean ripe(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
        IntegerProperty age = age(state);
        return age != null && state.getValue(age) >= Collections.max(age.getPossibleValues());
    }

    // a crop on the program's filter: its seed, its own item, or wheat and beetroot for their crops
    static boolean allowed(@Nullable GolemProgram p, Block crop) {
        if (p == null || !p.hasFilter()) return true;
        for (ItemStack f : p.filter()) {
            if (f.isEmpty()) continue;
            if (f.is(crop.asItem()) || f.getItem() instanceof BlockItem bi && bi.getBlock() == crop) return true;
            if (f.is(Items.WHEAT) && crop == Blocks.WHEAT || f.is(Items.BEETROOT) && crop == Blocks.BEETROOTS) return true;
        }
        return false;
    }

    // the crop a seed plants, when it is one the farmer may plant
    private static @Nullable Block seedCrop(@Nullable GolemProgram p, ItemStack s) {
        if (s.isEmpty() || !(s.getItem() instanceof BlockItem bi)) return null;
        Block b = bi.getBlock();
        if (!(b instanceof CropBlock) && age(b.defaultBlockState()) == null) return null;
        return allowed(p, b) ? b : null;
    }

    private static boolean tilling(@Nullable GolemProgram p) {
        return p != null && p.is(2, "on");
    }

    // which harvested spots get a new seed: all, every other one, or none
    private static boolean replants(@Nullable GolemProgram p, BlockPos pos) {
        if (p == null || p.option(0) == 0) return true;
        return p.is(0, "half") && ((pos.getX() + pos.getZ()) & 1) == 0;
    }

    private static boolean tillable(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.DIRT_PATH)) && level.getBlockState(pos.above()).isAir();
    }

    private static boolean emptyFarmland(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).is(Blocks.FARMLAND);
    }

    // farmland stays wet within four blocks of water, so only land like that is tilled
    private static boolean nearWater(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
            if (level.getFluidState(p).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    private static int seedSlot(GolemEntity golem, @Nullable GolemProgram p, ServerLevel level, BlockPos pos) {
        for (int i = 0; i < GolemEntity.CARRY_SLOTS; i++) {
            Block crop = seedCrop(p, golem.getCarried().get(i));
            if (crop != null && crop.defaultBlockState().canSurvive(level, pos)) return i;
        }
        return -1;
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        GolemProgram prog = golem.program();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(area.min(), area.max())) {
            BlockState s = level.getBlockState(p);
            if (s.isAir() || !ripe(s) || !allowed(prog, s.getBlock()) || skip.test(p)) continue;
            double d = golem.distanceToSqr(Vec3.atCenterOf(p));
            if (d < bestDist) {
                best = p.immutable();
                bestDist = d;
            }
        }
        wantsSeeds.remove(golem);
        if (best != null || !tilling(prog)) return best;
        // nothing ripe: plant empty farmland, then till wet land, nearest first
        boolean seeds = false;
        for (BlockPos p : BlockPos.betweenClosed(area.min().below(), area.max())) {
            if (skip.test(p)) continue;
            boolean plant = emptyFarmland(level, p) && replants(prog, p);
            if (plant && seedSlot(golem, prog, level, p) < 0) {
                seeds = true;
                continue;
            }
            if (!plant && !(tillable(level, p) && p.getY() < area.max().getY() && nearWater(level, p))) continue;
            double d = golem.distanceToSqr(Vec3.atCenterOf(p)) + (plant ? 0 : 4096);
            if (d < bestDist) {
                best = p.immutable();
                bestDist = d;
            }
        }
        if (seeds) wantsSeeds.put(golem, true);
        return best;
    }

    private static final Map<GolemEntity, Boolean> wantsSeeds = new WeakHashMap<>();

    @Override
    public @Nullable Predicate<ItemStack> restock(GolemEntity golem) {
        GolemProgram prog = golem.program();
        if (!tilling(prog) || !wantsSeeds.containsKey(golem)) return null;
        return s -> seedCrop(prog, s) != null;
    }

    @Override
    public @Nullable String supplies() {
        return "seeds";
    }

    // with tilling on, the biggest stack of seeds stays with it for planting
    @Override
    public int keep(GolemEntity golem) {
        GolemProgram prog = golem.program();
        if (!tilling(prog)) return -1;
        int best = -1;
        for (int i = 0; i < GolemEntity.CARRY_SLOTS; i++) {
            ItemStack c = golem.getCarried().get(i);
            if (seedCrop(prog, c) != null && (best < 0 || c.getCount() > golem.getCarried().get(best).getCount())) best = i;
        }
        return best;
    }

    @Override
    public int reserve(GolemEntity golem) {
        return SEED_KEEP;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return ripe(level.getBlockState(pos));
    }

    @Override
    public boolean isValid(GolemEntity golem, ServerLevel level, BlockPos pos) {
        GolemProgram prog = golem.program();
        if (ripe(level.getBlockState(pos))) return allowed(prog, level.getBlockState(pos).getBlock());
        if (!tilling(prog)) return false;
        return emptyFarmland(level, pos) ? seedSlot(golem, prog, level, pos) >= 0 : tillable(level, pos);
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return 10;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        GolemProgram prog = golem.program();
        BlockState state = level.getBlockState(pos);
        if (tilling(prog) && !ripe(state)) {
            if (emptyFarmland(level, pos)) {
                int slot = seedSlot(golem, prog, level, pos);
                if (slot < 0) return;
                BlockState crop = seedCrop(prog, golem.getCarried().get(slot)).defaultBlockState();
                level.setBlockAndUpdate(pos, crop);
                level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.8F, 1.0F);
                golem.getCarried().get(slot).shrink(1);
                golem.carriedChanged();
            } else if (tillable(level, pos) && !golem.getTool().isEmpty()) {
                level.setBlockAndUpdate(pos, Blocks.FARMLAND.defaultBlockState());
                level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                Mc.wear(golem.getTool(), golem);
            }
            return;
        }
        if (!ripe(state)) return;
        boolean replant = replants(prog, pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), golem, golem.getTool());
        Item seed = state.getBlock().asItem();
        if (replant) {
            for (ItemStack d : drops) {
                if (d.is(seed)) {
                    d.shrink(1);
                    break;
                }
            }
        }
        BlockState after = !replant ? Blocks.AIR.defaultBlockState()
                : state.getBlock() instanceof CropBlock crop ? crop.getStateForAge(0) : state.setValue(age(state), Collections.min(age(state).getPossibleValues()));
        level.levelEvent(2001, pos, Block.getId(state));
        level.setBlockAndUpdate(pos, after);
        Mc.wear(golem.getTool(), golem);
        if (prog != null && prog.is(1, "compost")) compost(golem, level, drops);
        Vec3 at = Vec3.atBottomCenterOf(pos).add(0, 0.3, 0);
        for (ItemStack d : LumberWork.merge(drops)) Storage.collectOrDrop(golem, level, at, d);
    }

    private static final int SEED_KEEP = 16;

    // spare seeds (ones that are not food) go into a composter in the area rather than the chest, once it carries enough to plant with
    private static void compost(GolemEntity golem, ServerLevel level, List<ItemStack> drops) {
        BlockPos bin = null;
        for (BlockPos p : BlockPos.betweenClosed(golem.getArea().min().offset(-Storage.NEARBY, -1, -Storage.NEARBY), golem.getArea().max().offset(Storage.NEARBY, 1, Storage.NEARBY))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof ComposterBlock && s.getValue(ComposterBlock.LEVEL) < ComposterBlock.MAX_LEVEL) {
                bin = p.immutable();
                break;
            }
        }
        if (bin == null) return;
        for (ItemStack d : drops) {
            if (d.isEmpty() || !ComposterBlock.COMPOSTABLES.containsKey(d.getItem()) || isFood(d) || !(d.getItem() instanceof BlockItem)) continue;
            int carried = 0;
            for (ItemStack c : golem.getCarried()) if (Mc.sameItem(c, d)) carried += c.getCount();
            while (!d.isEmpty() && (carried >= SEED_KEEP || !tilling(golem.program()))) {
                BlockState s = level.getBlockState(bin);
                if (!(s.getBlock() instanceof ComposterBlock) || s.getValue(ComposterBlock.LEVEL) >= ComposterBlock.MAX_LEVEL) return;
                ComposterBlock.insertItem(golem, s, level, d, bin);
            }
        }
    }

    private static boolean isFood(ItemStack s) {
        //? if >=1.20.5 {
        return s.has(net.minecraft.core.component.DataComponents.FOOD);
        //?} else {
        /*return s.getItem().isEdible();
        *///?}
    }
}
