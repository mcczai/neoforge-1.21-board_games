package net.mcczai.cardduel.resources;

import com.google.common.collect.Maps;
import net.mcczai.cardduel.resources.pojo.CardDataPOJO;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public enum CardAssetManager {
    INSTANCE;

    private final Map<ResourceLocation, CardDataPOJO> cardData = Maps.newHashMap();

    public void putCardData(ResourceLocation registryName, CardDataPOJO data){
        cardData.put(registryName,data);
    }

    public CardDataPOJO getCardData(ResourceLocation registryName){
        return cardData.get(registryName);
    }

    public void clearAll(){
        this.cardData.clear();
    }
}
