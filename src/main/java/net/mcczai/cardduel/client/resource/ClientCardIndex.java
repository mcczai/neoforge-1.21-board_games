package net.mcczai.cardduel.client.resource;

import com.google.common.base.Preconditions;
import net.mcczai.cardduel.resources.CardAssetManager;
import net.mcczai.cardduel.resources.pojo.CardIndexPOJO;
import net.mcczai.cardduel.resources.pojo.CardDataPOJO;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class ClientCardIndex {
    private String name;
    private String type;
    private CardDataPOJO cardDataPOJO;
    private @Nullable String tooltipKey;
    private int stackSize;
    private CardIndexPOJO pojo;

    private ClientCardIndex(){}

    public static @NotNull ClientCardIndex getInstance(CardIndexPOJO cardIndexPOJO) throws IllegalArgumentException{
        ClientCardIndex index = new ClientCardIndex();
        CheckIndex(cardIndexPOJO,index);
        checkData(cardIndexPOJO,index);
        CheckName(cardIndexPOJO,index);
        return index;
    }

    private static void CheckIndex(@NotNull CardIndexPOJO cardIndexPOJO, @NotNull ClientCardIndex index){
        Preconditions.checkArgument(cardIndexPOJO != null,"index object file is empty");
        Preconditions.checkArgument(StringUtils.isNoneBlank(cardIndexPOJO.getType()), "index object missing type field");
        index.type = cardIndexPOJO.getType();
    }

    public static void CheckName(@NotNull CardIndexPOJO cardIndexPOJO, @NotNull ClientCardIndex index){
        index.name = cardIndexPOJO.getName();
        if (StringUtils.isBlank(index.name)){
            index.name = "custom.cardduel.err.no_name";
        }
    }

    private static void checkData(@NotNull CardIndexPOJO cardIndexPOJO, @NotNull ClientCardIndex index) {
        ResourceLocation pojoData = cardIndexPOJO.getData();
        Preconditions.checkArgument(pojoData != null, "index object missing pojoData field");
        CardDataPOJO data = CardAssetManager.INSTANCE.getCardData(pojoData);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        index.cardDataPOJO = data;
    }



    public String getType() {
        return type;
    }

    public String getName(){
        return name;
    }

    public CardDataPOJO getCardData(){
        return cardDataPOJO;
    }

    public CardIndexPOJO getPojo() {
        return pojo;
    }
}

