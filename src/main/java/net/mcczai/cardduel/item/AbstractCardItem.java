package net.mcczai.cardduel.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractCardItem extends Item implements ICard {
    public AbstractCardItem(Properties properties) {
        super(properties);
    }

}
