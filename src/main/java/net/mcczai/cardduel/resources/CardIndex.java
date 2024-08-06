package net.mcczai.cardduel.resources;

import com.google.common.base.Preconditions;

public class CardIndex {
    private int stackSize;
    private CardIndexPOJO pojo;

    private CardIndex() {
    }

    public static CardIndex getInstance(CardIndexPOJO ammoIndexPOJO) throws IllegalArgumentException {
        CardIndex index = new CardIndex();
        index.pojo = ammoIndexPOJO;
        checkIndex(ammoIndexPOJO, index);
        return index;
    }

    private static void checkIndex(CardIndexPOJO ammoIndexPOJO, CardIndex index) {
        Preconditions.checkArgument(ammoIndexPOJO != null, "index object file is empty");
        index.stackSize = Math.max(ammoIndexPOJO.getStackSize(), 1);
    }

    public int getStackSize() {
        return stackSize;
    }

    public CardIndexPOJO getPojo() {
        return pojo;
    }
}
