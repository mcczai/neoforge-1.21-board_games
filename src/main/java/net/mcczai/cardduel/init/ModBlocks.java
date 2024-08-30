package net.mcczai.cardduel.init;

import net.mcczai.cardduel.block.DuelTableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.mcczai.cardduel.CardduelMod.MODID;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);

    public static final DeferredBlock<Block> DUELTABLE_BLOCK = BLOCKS.registerBlock("dueltable_block", DuelTableBlock::new,BlockBehaviour.Properties.of());

}
