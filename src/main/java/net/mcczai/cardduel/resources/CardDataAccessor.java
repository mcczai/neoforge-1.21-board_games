package net.mcczai.cardduel.resources;

import net.mcczai.cardduel.item.ICard;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public interface CardDataAccessor extends ICard{
    String CARD_NAME = "CardId";

    default ResourceLocation getCardId(ItemStack card){
       Component data = card.get(DataComponents.ITEM_NAME);
       if (data != null){
           ResourceLocation cardId = ResourceLocation.tryParse(CARD_NAME);
           return Objects.requireNonNullElse(cardId,DefaultAssets.EMPTY_CARD_ID);
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



}
