package net.mcczai.cardduel.client.resource;

import com.google.common.base.Preconditions;
import net.mcczai.cardduel.resources.CardAssetManager;
import net.mcczai.cardduel.resources.pojo.CardIndexPOJO;
import net.mcczai.cardduel.resources.pojo.data.CardData;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class ClientCardIndex {
    private String name;
    private String type;
    private CardData cardData;
    private @Nullable String tooltipKey;
    private int stackSize;
    private CardIndexPOJO pojo;

    private ClientCardIndex(){}

    public static ClientCardIndex getInstance(CardIndexPOJO cardIndexPOJO) throws IllegalArgumentException{
        ClientCardIndex index = new ClientCardIndex();
        CheckIndex(cardIndexPOJO,index);
        CheckName(cardIndexPOJO,index);
        return index;
    }

    private static void CheckIndex(CardIndexPOJO cardIndexPOJO,ClientCardIndex index){
        Preconditions.checkArgument(cardIndexPOJO != null,"index object file is empty");
        Preconditions.checkArgument(StringUtils.isNoneBlank(cardIndexPOJO.getType()), "index object missing type field");
        index.type = cardIndexPOJO.getType();
    }

    public static void CheckName(CardIndexPOJO cardIndexPOJO,ClientCardIndex index){
        index.name = cardIndexPOJO.getName();
        if (StringUtils.isBlank(index.name)){
            index.name = "custom.cardduel.err.no_name";
        }
    }

    private static void checkData(CardIndexPOJO cardIndexPOJO, ClientCardIndex index) {
        ResourceLocation pojoData = cardIndexPOJO.getData();
        Preconditions.checkArgument(pojoData != null, "index object missing pojoData field");
        CardData data = CardAssetManager.INSTANCE.getCardData(pojoData);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        // 剩下的不需要校验了，Common的读取逻辑中已经校验过了
        index.cardData = data;
    }


    public String getType() {
        return type;
    }

    public String getName(){
        return name;
    }

    public CardData getCardData(){
        return cardData;
    }

    public CardIndexPOJO getPojo() {
        return pojo;
    }
}

