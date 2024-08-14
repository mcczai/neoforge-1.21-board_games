package net.mcczai.cardduel.resources;

import com.google.common.collect.Maps;
import net.mcczai.cardduel.resources.pojo.data.CardData;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public enum CardAssetManager {
    INSTANCE;

    private final Map<ResourceLocation, CardData> cardData = Maps.newHashMap();

    public void putCardData(ResourceLocation registryName,CardData data){
        cardData.put(registryName,data);
    }

    public CardData getCardData(ResourceLocation registryName){
        return cardData.get(registryName);
    }

    public void clearAll(){
        this.cardData.clear();
    }
}
