package net.mcczai.cardduel.API;

import net.mcczai.cardduel.client.resource.ClientCardIndex;
import net.mcczai.cardduel.client.resource.ClientCardPackLoader;
import net.mcczai.cardduel.resources.CommonCardIndex;
import net.mcczai.cardduel.resources.CommonCardPackLoader;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CdAPI {
    @OnlyIn(Dist.CLIENT)
    public static Optional<ClientCardIndex> getClientCardIndex(ResourceLocation cardId){
        return ClientCardPackLoader.getCardIndex(cardId);
    }

    @OnlyIn(Dist.CLIENT)
    public static Set<Map.Entry<ResourceLocation,ClientCardIndex>> getAllClientCardIndex(){
        return ClientCardPackLoader.getAllCard();
    }

    public static Optional<CommonCardIndex> getCommonCardIndex(ResourceLocation cardId){
        return CommonCardPackLoader.getCardIndex(cardId);
    }

    public static Set<Map.Entry<ResourceLocation,CommonCardIndex>> getAllCommonCardIndex(){
        return CommonCardPackLoader.getAllCards();
    }


}
