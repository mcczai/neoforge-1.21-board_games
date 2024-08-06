package net.mcczai.cardduel.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.mcczai.cardduel.CardduelMod.MODID;

public class ModBlock {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);

    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));

}
