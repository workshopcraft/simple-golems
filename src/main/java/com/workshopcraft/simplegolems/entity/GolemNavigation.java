package com.workshopcraft.simplegolems.entity;

import com.workshopcraft.simplegolems.Mc;
import com.workshopcraft.simplegolems.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathFinder;
//? if >=1.20.5 {
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
//?} else {
/*import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
*///?}
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

// golems keep off treetops and out of lava, fire, magma and the void: no standing on leaves, lumber never on a trunk, miners keep out of water
public class GolemNavigation extends GroundPathNavigation {
    private static final int SAFE_FALL = 3;

    private final GolemEntity golem;

    public GolemNavigation(GolemEntity golem, Level level) {
        super(golem, level);
        this.golem = golem;
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        nodeEvaluator = new WalkNodeEvaluator() {
            @Override
            //? if >=1.20.5 {
            public PathType getPathType(PathfindingContext context, int x, int y, int z) {
                PathType type = super.getPathType(context, x, y, z);
            //?} else {
            /*public BlockPathTypes getBlockPathType(BlockGetter context, int x, int y, int z) {
                BlockPathTypes type = super.getBlockPathType(context, x, y, z);
            *///?}
                //? if >=1.21.2 {
                if (type == PathType.LAVA || type == PathType.FIRE) return PathType.BLOCKED;
                //?} else {
                /*if (type == PathType.LAVA || type == PathType.DAMAGE_FIRE) return PathType.BLOCKED;
                *///?}
                // a miner stays out of water it might not climb out of
                if (golem.getJob() == Job.MINER && type == PathType.WATER) return PathType.BLOCKED;
                if (type != PathType.WALKABLE && type != PathType.OPEN) return type;
                BlockPos below = new BlockPos(x, y - 1, z);
                //? if >=1.20.5 {
                BlockState floor = context.getBlockState(below);
                BlockState under = context.getBlockState(below.below());
                //?} else {
                /*BlockState floor = context.getBlockState(below);
                BlockState under = context.getBlockState(below.below());
                *///?}
                if (hotFloor(floor) || offLimits(floor, under) || voidEdge(x, y, z)) return PathType.BLOCKED;
                return type;
            }
        };
        return new PathFinder(nodeEvaluator, maxVisitedNodes);
    }

    private static boolean hotFloor(BlockState floor) {
        return floor.is(BlockTags.FIRE) || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.LAVA)
                || floor.getFluidState().is(FluidTags.LAVA);
    }

    // no sturdy ground within a short fall, or near world bottom: do not walk into the void / deep drops
    private boolean voidEdge(int x, int y, int z) {
        Level level = golem.level();
        int min = Mc.minY(level);
        if (y - 1 <= min + 1) return true;
        for (int dy = 1; dy <= SAFE_FALL; dy++) {
            BlockPos p = new BlockPos(x, y - dy, z);
            BlockState s = level.getBlockState(p);
            if (hotFloor(s)) return true;
            if (s.isFaceSturdy(level, p, Direction.UP) && s.getFluidState().isEmpty()) return false;
            if (!s.getCollisionShape(level, p).isEmpty()) return false;
        }
        return true;
    }

    private boolean offLimits(BlockState floor, BlockState under) {
        if (floor.getBlock() instanceof LeavesBlock) return true;
        return golem.getJob() == Job.LUMBER && floor.is(ModTags.LOGS) && (under.is(ModTags.LOGS) || under.is(BlockTags.SUBSTRATE_OVERWORLD));
    }
}
