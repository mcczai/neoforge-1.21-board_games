package net.mcczai.cardduel.item;

import net.mcczai.cardduel.resources.CardDataAccessor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CardItem extends Item implements CardDataAccessor {

    public CardItem(Properties properties) {
        super(new Properties().stacksTo(1));
    }

    @Override
    public int getHp(ItemStack Card) {
        return 0;
    }

    @Override
    public int getMp(ItemStack Card) {
        return 0;
    }

    @Override
    public int getAtk(ItemStack Card) {
        return 0;
    }

    @Override
    public int getType(ItemStack Card) {
        return 0;
    }

    @Override
    public String getSkill(ItemStack Card) {
        return "12";
    }

}
