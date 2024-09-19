package net.mcczai.cardduel.block;

import com.mojang.serialization.MapCodec;
import net.mcczai.cardduel.block.entity.DuelTableBlockEntity;
import net.mcczai.cardduel.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class DuelTableBlock extends BaseEntityBlock {

    public static final MapCodec<DuelTableBlock> CODEC = simpleCodec(DuelTableBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty DOUBLE = BooleanProperty.create("double");

    public DuelTableBlock(@NotNull Properties properties) {
        super(properties.noOcclusion());
        this.registerDefaultState(
            this.stateDefinition
                    .any()
                    .setValue(FACING,Direction.NORTH)
                    .setValue(DOUBLE, false)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection();
        BlockGetter blockGetter = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        BlockPos blockPos1 = blockPos.relative(direction);
        BlockState blockState = blockGetter.getBlockState(blockPos);
        BlockState blockState1 = blockGetter.getBlockState(blockPos1);
            if (blockState.getBlock() == ModBlocks.DUELTABLE_BLOCK.get() && blockState1.getBlock() == ModBlocks.DUELTABLE_BLOCK.get()) {
                return this.defaultBlockState()
                        .setValue(DOUBLE, true)
                        .setValue(FACING, context.getHorizontalDirection());

            } else {
                return this.defaultBlockState()
                        .setValue(DOUBLE, false)
                        .setValue(FACING, context.getHorizontalDirection());
            }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING,DOUBLE);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new DuelTableBlockEntity(blockPos,blockState);
    }
}
