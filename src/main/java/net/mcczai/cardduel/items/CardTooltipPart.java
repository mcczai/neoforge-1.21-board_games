package net.mcczai.cardduel.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public enum CardTooltipPart {
    DESCRIPTION,
    ATK_INFO,
    HP_INFO,
    MP_INFO,
    TYPE_INFO;

    private final int mask = 1 << this.ordinal();

    public int getMask() {
        return this.mask;
    }

    public static  int getHideFlags(ItemStack stack){
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag != null && tag.contains("HideFlags", Tag.TAG_ANY_NUMERIC)){
            return tag.getInt("HideFlags");
        }
        return 0;
    }

    public static void setHideFlags(ItemStack stack,int mask){
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int itemHP = tag.getInt("HideFlags");
        tag.putInt("HideFlags",mask);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
}
