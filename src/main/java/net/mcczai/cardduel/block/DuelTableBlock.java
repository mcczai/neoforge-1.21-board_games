package net.mcczai.cardduel.block;

import com.mojang.serialization.MapCodec;
import net.mcczai.cardduel.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class DuelTableBlock extends BaseEntityBlock {

    public static final EnumProperty<TablePart> PART = EnumProperty.create("part", TablePart.class);

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final VoxelShape SHAPEE = Stream.of(
            Block.box(0, 15, 0, 16, 16, 16),
            Block.box(2, 0, 12, 4, 15, 14),
            Block.box(2, 0, 2, 4, 15, 4)
    ).reduce((v1, v2) -> Shapes.join(v1,v2, BooleanOp.OR)).get();
    public static final VoxelShape SHAPES = Stream.of(
            Block.box(0, 15, 0, 16, 16, 16),
            Block.box(2, 0, 2, 4, 15, 4),
            Block.box(12, 0, 2, 14, 15, 4)
    ).reduce((v1, v2) -> Shapes.join(v1,v2, BooleanOp.OR)).get();
    public static final VoxelShape SHAPEW = Stream.of(
            Block.box(0, 15, 0, 16, 16, 16),
            Block.box(2, 0, 12, 4, 15, 14),
            Block.box(2, 0, 2, 4, 15, 4)
    ).reduce((v1, v2) -> Shapes.join(v1,v2, BooleanOp.OR)).get();
    public static final VoxelShape SHAPEN = Stream.of(
            Block.box(0, 15, 0, 16, 16, 16),
            Block.box(2, 0, 2, 4, 15, 4),
            Block.box(12, 0, 2, 14, 15, 4)
    ).reduce((v1, v2) -> Shapes.join(v1,v2, BooleanOp.OR)).get();


    public DuelTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(PART,TablePart.FOOT));
    }

    private static Direction getNeighbourDirection(TablePart part, Direction direction) {
        return part == TablePart.FOOT ? direction : direction.getOpposite();
    }

    public static Direction getConnectedDirection(BlockState state) {
        Direction direction = state.getValue(FACING);
        return state.getValue(PART) == TablePart.HEAD ? direction.getOpposite() : direction;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING,PART);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection();
        BlockPos blockPos = context.getClickedPos();
        BlockPos blockPos1 = blockPos.relative(direction);
        Level level = context.getLevel();
        return level.getBlockState(blockPos1).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(blockPos1)
                ? this.defaultBlockState().setValue(FACING, direction)
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level,pos,state,placer,stack);
        if (!level.isClientSide){
            BlockPos blockPos = pos.relative(state.getValue(FACING));
            level.setBlock(blockPos,state.setValue(PART,TablePart.HEAD),2);
            level.blockUpdated(pos,Blocks.AIR);
            state.updateNeighbourShapes(level,pos,2);
        }
    }

    @Override
    public BlockState playerWillDestroy(@NotNull Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative()) {
            TablePart tablePart = state.getValue(PART);
            if (tablePart == tablePart.FOOT) {
                BlockPos blockpos = pos.relative(getNeighbourDirection(tablePart, state.getValue(FACING)));
                BlockState blockstate = level.getBlockState(blockpos);
                if (blockstate.is(this) && blockstate.getValue(PART) == TablePart.HEAD) {
                    level.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 35);
                    level.levelEvent(player, 2001, blockpos, Block.getId(blockstate));
                }
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, BlockGetter blockGetter, BlockPos blockPos, CollisionContext context) {
        Direction direction = getConnectedDirection(state).getOpposite();
        switch (direction) {
            case NORTH:
                return SHAPEN;
            case SOUTH:
                return SHAPES;
            case WEST:
                return SHAPEW;
            default:
                return SHAPEE;
        }
    }

    protected enum TablePart implements StringRepresentable {
        HEAD("head"),
        FOOT("foot");

        private final String name;

        private TablePart(String name){
            this.name = name;
        }

        public String toString() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

}
