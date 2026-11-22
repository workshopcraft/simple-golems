package com.workshopcraft.simplegolems.entity.goal;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

// a guard with a route on its program walks from point to point, pausing at each, and starts over at the first
public class PatrolGoal extends Goal {
    private static final int PAUSE = 40, GIVE_UP = 300;
    private final GolemEntity golem;
    private int next, ticks, pause;

    public PatrolGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return golem.canGuard() && golem.getTarget() == null && !golem.patrolRoute().isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        ticks = 0;
        List<BlockPos> route = golem.patrolRoute();
        if (next >= route.size()) next = 0;
        walk(route.get(next));
    }

    @Override
    public void stop() {
        golem.getNavigation().stop();
    }

    private void walk(BlockPos p) {
        golem.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.8);
    }

    @Override
    public void tick() {
        List<BlockPos> route = golem.patrolRoute();
        if (next >= route.size()) next = 0;
        BlockPos p = route.get(next);
        if (pause > 0) {
            if (--pause == 0) walk(p);
            return;
        }
        ticks++;
        boolean there = golem.distanceToSqr(Vec3.atBottomCenterOf(p)) < 2.5;
        // a point it cannot get to is passed over rather than walked at forever
        if (there || ticks > GIVE_UP) {
            next = (next + 1) % route.size();
            ticks = 0;
            if (there) {
                pause = PAUSE;
                golem.getNavigation().stop();
            } else {
                walk(route.get(next));
            }
        } else if (golem.getNavigation().isDone() && ticks % 20 == 0) {
            walk(p);
        }
    }
}
