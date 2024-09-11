package net.mcczai.cardduel.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class DuelTableBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public DuelTableBlock(Properties properties) {
        super(BlockBehaviour.Properties.of());
    }

    public static Object get() {
        return null;
    }


    public static final VoxelShape SHAPENS = Stream.of(
            Block.box(0, 15, 0, 16, 16, 32),
            Block.box(2, 0, 2, 4, 15, 4),
            Block.box(12, 0, 2, 14, 15, 4),
            Block.box(2, 0, 28, 4, 15, 30),
            Block.box(12, 0, 28, 14, 15, 30)
    ).reduce((v1, v2) -> Shapes.join(v1,v2, BooleanOp.OR)).get();

    public static final VoxelShape SHAPEWE = Stream.of(
            Block.box(0, 15, 1, 32, 16, 17),
            Block.box(2, 0, 13, 4, 15, 15),
            Block.box(2, 0, 3, 4, 15, 5),
            Block.box(28, 0, 13, 30, 15, 15),
            Block.box(28, 0, 3, 30, 15, 5)
    ).reduce((v1, v2) -> Shapes.join(v1,v2, BooleanOp.OR)).get();

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos blockPos, CollisionContext context) {
        if (state.getValue(FACING).getAxis() == Direction.Axis.Z)
            return SHAPENS;
        return SHAPEWE;
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


}
