package com.workshopcraft.simplegolems.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
//? if >=1.21.2 {
import net.minecraft.world.Containers;
import net.minecraft.server.level.ServerLevel;
//?} else {
/*import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
*///?}

public class ProgrammerBlock extends BaseEntityBlock {
    //? if >=1.20.3 {
    public static final MapCodec<ProgrammerBlock> CODEC = simpleCodec(ProgrammerBlock::new);
    //?}
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public ProgrammerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    //? if >=1.20.3 {
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    //?}

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProgrammerBlockEntity(pos, state);
    }

    @Override
    //? if >=1.21.2 {
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ProgrammerBlockEntity be) player.openMenu(be);
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ProgrammerBlockEntity be) player.openMenu(be);
        return InteractionResult.SUCCESS;
    }
    *///?}

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    //? if >=1.21.2 {
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
    //?} else {
    /*@Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ProgrammerBlockEntity be) {
            Containers.dropContents(level, pos, be);
        }
        super.onRemove(state, level, pos, next, moving);
    }
    *///?}

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    //? if >=1.20.3 {
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    //?} else {
    /*public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    *///?}
}
