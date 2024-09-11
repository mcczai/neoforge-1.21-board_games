package net.mcczai.cardduel.API.item.nbt;

import net.mcczai.cardduel.items.ICard;
import net.mcczai.cardduel.resources.DefaultAssets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Objects;

public interface CardDataAccessor extends ICard{
    String CARD_NAME = "CardId";

    default ResourceLocation getCardId(ItemStack card){
       Component data = card.get(DataComponents.ITEM_NAME);
       if (data != null){
           ResourceLocation cardId = ResourceLocation.tryParse(CARD_NAME);

           return Objects.requireNonNullElse(cardId, DefaultAssets.EMPTY_CARD_ID);
       }
        return DefaultAssets.EMPTY_CARD_ID;
    }

    default void setCardId(ItemStack card,ResourceLocation cardId){
        if (cardId != null){
            Component dataId = Component.nullToEmpty(cardId.toString());
            card.set(DataComponents.ITEM_NAME,dataId);
            return;
        }
        Component DefaultId = Component.nullToEmpty(DefaultAssets.EMPTY_CARD_ID.toString());
        card.set(DataComponents.ITEM_NAME,DefaultId);
    }

    default int getHP(ItemStack card){
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if (tag.get("HP") != null) {
            int Hp = tag.getInt("HP");
            return Hp;
        }
        return 1;
    }
    default void setHP(ItemStack card, int amount){
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("HP",Math.max(amount,1));
        card.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }

    @Override
    default int getMP(ItemStack card){
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if (tag.get("MP") != null) {
            int Mp = tag.getInt("MP");
            return Mp;
        }
        return 1;
    }

    default void setMP(ItemStack card, int amount){
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("MP",Math.max(amount,1));
        card.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }

    @Override
    default int getATK(ItemStack card){
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if (tag.get("ATK") != null) {
            int Atk = tag.getInt("ATK");
            return Atk;
        }
        return 1;
    }

    default void setATK(ItemStack card, int amount){
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("ATK",Math.max(amount,1));
        card.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }

    @Override
    default int getType(ItemStack card){
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if (tag.get("Type") != null) {
            int type = tag.getInt("Type");
            return type;
        }
        return 0;
    }

    default void setType(ItemStack card,int type){
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("Type",Math.max(type,0));
        card.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }

    @Override
    default String getSkill(ItemStack card){
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.get("Skill") != null) {
            String skill = tag.getString("Skill");
            return skill;
        }
        return "0";
    }

    @Override
    default void setSkill(ItemStack card, String skill){
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString("Skill",skill);
        card.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
}
