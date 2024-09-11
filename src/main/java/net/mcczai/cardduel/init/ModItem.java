package net.mcczai.cardduel.init;

import net.mcczai.cardduel.items.CardBundleItem;
import net.mcczai.cardduel.items.CardItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.mcczai.cardduel.CardduelMod.MODID;

public class ModItem {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<CardItem> CARD_ITEM = ITEMS.register("card", ()-> new CardItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<CardBundleItem> CARD_BUNDLE_ITEM = ITEMS.register("card_bundle",()->
            new CardBundleItem(
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DUELTABLE_BLOCK_ITEM = ITEMS.register("dueltable_block_item",()->
            new BlockItem(
                    ModBlocks.DUELTABLE_BLOCK.get(),new Item.Properties().stacksTo(1)));


}
