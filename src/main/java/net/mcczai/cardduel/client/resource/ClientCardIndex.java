package net.mcczai.cardduel.client.resource;

import com.google.common.base.Preconditions;
import net.mcczai.cardduel.resources.CardIndexPOJO;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class ClientCardIndex {
    private String name;
    private @Nullable String tooltipKey;
    private int stackSize;
    private CardIndexPOJO pojo;

    private ClientCardIndex(){}

    public static ClientCardIndex getInstance(CardIndexPOJO cardIndexPOJO) throws IllegalArgumentException{
        ClientCardIndex index = new ClientCardIndex();
        CheckIndex(cardIndexPOJO,index);
        CheckName(cardIndexPOJO,index);
        CheckStackSize(cardIndexPOJO,index);
        return index;
    }

    private static void CheckIndex(CardIndexPOJO cardIndexPOJO,ClientCardIndex index){
        Preconditions.checkArgument(cardIndexPOJO != null,"index object file is empty");
        Preconditions.checkArgument(StringUtils.isNoneBlank(cardIndexPOJO.getType()), "index object missing type field");
        index.tooltipKey = cardIndexPOJO.getTooltip();
    }

    public static void CheckName(CardIndexPOJO cardIndexPOJO,ClientCardIndex index){
        index.name = cardIndexPOJO.getName();
        if (StringUtils.isBlank(index.name)){
            index.name = "custom.cardduel.err.no_name";
        }
    }

    public static void CheckStackSize(CardIndexPOJO cardIndexPOJO, ClientCardIndex index) {
        index.stackSize = Math.max(cardIndexPOJO.getStackSize(),1);
    }

    public String getName(){
        return name;
    }

    @Nullable
    public String getTooltipKey() {
        return tooltipKey;
    }

    public CardIndexPOJO getPojo() {
        return pojo;
    }
}

