package net.mcczai.cardduel.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.mcczai.cardduel.CardduelMod.MODID;
import static net.mcczai.cardduel.init.ModBlock.EXAMPLE_BLOCK;

public class ModItem {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<Item> CARD_ITEM = ITEMS.registerSimpleItem("card", new Item.Properties());

    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

}
