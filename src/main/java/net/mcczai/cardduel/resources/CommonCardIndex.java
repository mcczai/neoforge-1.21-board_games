package net.mcczai.cardduel.resources;

import com.google.common.base.Preconditions;
import net.mcczai.cardduel.resources.pojo.data.CardData;
import net.mcczai.cardduel.resources.pojo.CardIndexPOJO;
import net.minecraft.resources.ResourceLocation;

public class CommonCardIndex {

    private CardData cardData;
    private String type;
    private int stackSize;
    private CardIndexPOJO pojo;

    private CommonCardIndex() {
    }

    public static CommonCardIndex getInstance(CardIndexPOJO CardIndexPOJO) throws IllegalArgumentException {
        CommonCardIndex index = new CommonCardIndex();
        index.pojo = CardIndexPOJO;
        checkIndex(CardIndexPOJO, index);
        checkData(CardIndexPOJO,index);
        return index;
    }

    private static void checkIndex(CardIndexPOJO cardIndexPOJO, CommonCardIndex index) {
        Preconditions.checkArgument(cardIndexPOJO != null, "index object file is empty");
        index.stackSize = Math.max(cardIndexPOJO.getStackSize(), 1);
    }

    private static void checkData(CardIndexPOJO cardIndexPOJO,CommonCardIndex index){
        ResourceLocation pojoData = cardIndexPOJO.getData();
        Preconditions.checkArgument(pojoData != null,"index object missing pojoData field");
        CardData data = CardAssetManager.INSTANCE.getCardData(pojoData);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        Preconditions.checkArgument(data.getATK() >= 1, "HP count must >= 1");
        Preconditions.checkArgument(data.getHP() >= 1, "HP count must >= 1");
        Preconditions.checkArgument(data.getMP() >= 1, "HP count must >= 1");
        Preconditions.checkArgument(data.getDESCRIPTION() != null, "description id is empty");
        Preconditions.checkArgument(data.getTYPE() >= 0, "type id is empty");
        Preconditions.checkArgument(data.getSKILL() != null, "skill id is empty");
    }


    public CardData getCardData(){
        return cardData;
    }

    public String getType(){
        return type;
    }
    public CardIndexPOJO getPojo() {
        return pojo;
    }
}
