package net.mcczai.cardduel.block.entity;

import com.mojang.serialization.MapCodec;
import net.mcczai.cardduel.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DuelTableBlockEntity extends BlockEntity {


    public DuelTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DUELTABLE.get(), pos, blockState);
    }
}
