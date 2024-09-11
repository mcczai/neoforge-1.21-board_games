package net.mcczai.cardduel.block.entity;

import net.mcczai.cardduel.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DuelTableBlockEntity extends BlockEntity {


    public DuelTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DUELTABLE.get(), pos, blockState);
    }


}
