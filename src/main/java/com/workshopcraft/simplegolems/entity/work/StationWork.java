package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.Mc;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// programs that work between blocks clicked with the marker stick: the first carry slots hold what it works with, the rest what it makes
public abstract class StationWork implements JobWork {
    // carry slots below this hold inputs, the others results
    protected int inputs() {
        return 1;
    }

    // the clicked block the results go to
    protected abstract int output();

    public static @Nullable BlockPos point(GolemEntity golem, int i) {
        List<BlockPos> p = golem.getPoints();
        return i < p.size() ? p.get(i) : null;
    }

    @Override
    public boolean wants(ItemEntity item) {
        return false;
    }

    @Override
    public boolean keeps(GolemEntity golem, int slot) {
        return slot < inputs();
    }

    @Override
    public List<ItemStack> load(GolemEntity golem) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = inputs(); i < GolemEntity.CARRY_SLOTS; i++) {
            ItemStack c = golem.getCarried().get(i);
            if (!c.isEmpty()) out.add(c);
        }
        return out;
    }

    @Override
    public @Nullable BlockPos dropOff(GolemEntity golem, ServerLevel level, WorkArea area, ItemStack carried, Predicate<BlockPos> skip) {
        BlockPos to = point(golem, output());
        if (to == null || skip.test(to)) return null;
        Container c = Storage.containerAt(level, to);
        return c != null && Storage.hasRoom(c, carried) ? to : null;
    }

    protected boolean room(GolemEntity golem) {
        for (int i = inputs(); i < GolemEntity.CARRY_SLOTS; i++) if (golem.getCarried().get(i).isEmpty()) return true;
        return false;
    }

    protected ItemStack input(GolemEntity golem, int slot) {
        return golem.getCarried().get(slot);
    }

    // what does not fit stays on the ground for the next turn
    protected void addResult(GolemEntity golem, ServerLevel level, ItemStack stack, Vec3 at) {
        for (int i = inputs(); i < GolemEntity.CARRY_SLOTS && !stack.isEmpty(); i++) {
            ItemStack c = golem.getCarried().get(i);
            if (!c.isEmpty() && Mc.sameItem(c, stack)) {
                int n = Math.min(stack.getCount(), c.getMaxStackSize() - c.getCount());
                c.grow(n);
                stack.shrink(n);
            }
        }
        for (int i = inputs(); i < GolemEntity.CARRY_SLOTS && !stack.isEmpty(); i++) {
            if (golem.getCarried().get(i).isEmpty()) {
                golem.getCarried().set(i, stack.split(stack.getMaxStackSize()));
            }
        }
        golem.carriedChanged();
        Storage.spawnWorkDrop(level, at, stack);
    }

    // how many of a stack the result slots can take
    protected int fits(GolemEntity golem, ItemStack stack) {
        int n = 0;
        for (int i = inputs(); i < GolemEntity.CARRY_SLOTS; i++) {
            ItemStack c = golem.getCarried().get(i);
            if (c.isEmpty()) n += stack.getMaxStackSize();
            else if (Mc.sameItem(c, stack)) n += c.getMaxStackSize() - c.getCount();
        }
        return n;
    }

    // results lying on or just above a block, such as a sieve's
    protected void collect(GolemEntity golem, ServerLevel level, BlockPos pos) {
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0, 1.5, 1.0), ItemEntity::isAlive)) {
            ItemStack s = e.getItem();
            int n = Math.min(s.getCount(), fits(golem, s));
            if (n <= 0) continue;
            addResult(golem, level, s.split(n), e.position());
            if (s.isEmpty()) e.discard();
            else e.setItem(s);
        }
    }

    public static boolean has(Container c, Predicate<ItemStack> want) {
        for (int i = 0; i < c.getContainerSize(); i++) if (!c.getItem(i).isEmpty() && want.test(c.getItem(i))) return true;
        return false;
    }

    // a stack of the first kind of item that passes the test, into an input slot
    protected boolean take(GolemEntity golem, ServerLevel level, BlockPos from, int slot, Predicate<ItemStack> want) {
        Container c = Storage.containerAt(level, from);
        if (c == null) return false;
        boolean[] ok = {false};
        golem.useStorage(from, () -> {
            ItemStack got = golem.getCarried().get(slot);
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.getItem(i);
                if (s.isEmpty() || !want.test(s) || !got.isEmpty() && !Mc.sameItem(got, s)) continue;
                int room = got.isEmpty() ? s.getMaxStackSize() : got.getMaxStackSize() - got.getCount();
                if (room <= 0) break;
                ItemStack t = c.removeItem(i, Math.min(room, s.getCount()));
                if (got.isEmpty()) got = t;
                else got.grow(t.getCount());
            }
            golem.getCarried().set(slot, got);
            golem.carriedChanged();
            c.setChanged();
            if (!got.isEmpty()) golem.playSound(SoundEvents.BUNDLE_INSERT, 0.6F, 1.2F);
            ok[0] = !got.isEmpty();
        });
        return ok[0];
    }

    protected static boolean sourceHas(ServerLevel level, @Nullable BlockPos from, Predicate<ItemStack> want) {
        if (from == null) return false;
        Container c = Storage.containerAt(level, from);
        return c != null && has(c, want);
    }

    // stand beside the work cell, never in it: empty fluids so cobblegen is not lava-pushed and hammer can place
    protected static @Nullable BlockPos standBeside(Level level, BlockPos target) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos p = target.relative(d).above(dy), below = p.below();
                if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() || !level.getFluidState(p).isEmpty()) continue;
                if (!level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty() || !level.getFluidState(p.above()).isEmpty()) continue;
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || !level.getFluidState(below).isEmpty()) continue;
                double d2 = p.distSqr(target);
                if (d2 < bestDist) {
                    best = p.immutable();
                    bestDist = d2;
                }
            }
        }
        return best;
    }

    // get out of the work cell so a block can be placed there; snap beside when possible
    protected static boolean clearSpot(GolemEntity golem, Level level, BlockPos spot) {
        if (!golem.getBoundingBox().intersects(new AABB(spot))) return true;
        BlockPos aside = standBeside(level, spot);
        if (aside != null) {
            golem.getNavigation().stop();
            Mc.place(golem, aside.getX() + 0.5, aside.getY(), aside.getZ() + 0.5, golem.getYRot(), golem.getXRot());
            golem.setDeltaMovement(Vec3.ZERO);
            return !golem.getBoundingBox().intersects(new AABB(spot));
        }
        // no free neighbor: shove horizontally away from the spot center
        Vec3 c = Vec3.atCenterOf(spot);
        double dx = golem.getX() - c.x, dz = golem.getZ() - c.z;
        if (dx * dx + dz * dz < 1.0E-4) {
            dx = 1.0;
            dz = 0.0;
        }
        double len = Math.sqrt(dx * dx + dz * dz);
        golem.setDeltaMovement(dx / len * 0.45, golem.getDeltaMovement().y, dz / len * 0.45);
        return false;
    }
}
