package net.mcczai.cardduel.resources;

import com.google.common.base.Preconditions;
import net.minecraft.world.item.DyeColor;

public class CardIndex {

    private int stackSize;
    private CardIndexPOJO pojo;

    private CardIndex() {
    }

    public static CardIndex getInstance(CardIndexPOJO CardIndexPOJO) throws IllegalArgumentException {
        CardIndex index = new CardIndex();
        index.pojo = CardIndexPOJO;
        checkIndex(CardIndexPOJO, index);
        return index;
    }

    private static void checkIndex(CardIndexPOJO cardIndexPOJO, CardIndex index) {
        Preconditions.checkArgument(cardIndexPOJO != null, "index object file is empty");
        index.stackSize = Math.max(cardIndexPOJO.getStackSize(), 1);
    }

    public int getStackSize() {
        return stackSize;
    }

    public CardIndexPOJO getPojo() {
        return pojo;
    }
}
