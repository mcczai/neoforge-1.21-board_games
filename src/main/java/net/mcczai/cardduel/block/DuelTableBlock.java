package net.mcczai.cardduel.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DuelTableBlock extends BaseEntityBlock {

    public DuelTableBlock(Properties properties) {
        super(BlockBehaviour.Properties.of());
    }

    public static Object get() {
        return null;
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
