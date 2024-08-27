package net.mcczai.cardduel.item;

import net.mcczai.cardduel.resources.CardDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class CardItem extends AbstractCardItem implements CardDataAccessor {

    public CardItem(Properties properties) {
        super(new Properties().stacksTo(1));
    }

}

