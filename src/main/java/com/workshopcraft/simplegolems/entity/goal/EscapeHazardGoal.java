package com.workshopcraft.simplegolems.entity.goal;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

// in lava, fire or burning: head for the nearest safe dry spot (direct steer if pathfinding is blocked by lava nodes)
public class EscapeHazardGoal extends Goal {
    private final GolemEntity golem;
    private @Nullable BlockPos safe;

    public EscapeHazardGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    private boolean inDanger() {
        return golem.isInLava() || golem.isOnFire() || golem.level().getBlockState(golem.blockPosition()).is(BlockTags.FIRE)
                || golem.level().getBlockState(golem.getOnPos()).is(Blocks.MAGMA_BLOCK);
    }

    @Override
    public boolean canUse() {
        if (!inDanger()) return false;
        safe = findSafe(golem.level(), golem.blockPosition());
        return safe != null;
    }

    @Override
    public boolean canContinueToUse() {
        return safe != null && inDanger();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        moveToward();
    }

    @Override
    public void tick() {
        if (safe == null) return;
        if (golem.blockPosition().closerThan(safe, 1.5) && !inDanger()) return;
        moveToward();
    }

    private void moveToward() {
        if (safe == null) return;
        double x = safe.getX() + 0.5, y = safe.getY(), z = safe.getZ() + 0.5;
        Path path = golem.getNavigation().createPath(safe, 0);
        if (path != null && path.canReach()) {
            golem.getNavigation().moveTo(path, 1.4);
            return;
        }
        // lava/fire nodes are BLOCKED for normal work, so steer out directly when pathfinding cannot leave
        golem.getNavigation().stop();
        golem.getMoveControl().setWantedPosition(x, y, z, 1.4);
        if (golem.onGround() || golem.isInLava()) golem.getJumpControl().jump();
    }

    private static @Nullable BlockPos findSafe(Level level, BlockPos from) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(from.offset(-8, -2, -8), from.offset(8, 4, 8))) {
            if (!clear(level, p) || !clear(level, p.above())) continue;
            BlockPos below = p.below();
            BlockState floor = level.getBlockState(below);
            if (!floor.isFaceSturdy(level, below, Direction.UP) || !level.getFluidState(below).isEmpty()) continue;
            if (floor.is(BlockTags.FIRE) || floor.is(Blocks.MAGMA_BLOCK)) continue;
            double d = p.distSqr(from);
            if (d < bestDist) {
                best = p.immutable();
                bestDist = d;
            }
        }
        return best;
    }

    private static boolean clear(Level level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.getCollisionShape(level, p).isEmpty() && s.getFluidState().isEmpty() && !s.is(BlockTags.FIRE);
    }
}
