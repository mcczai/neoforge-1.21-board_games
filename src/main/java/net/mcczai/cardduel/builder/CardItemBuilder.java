package net.mcczai.cardduel.builder;

public class CardItemBuilder {
    private int count = 1;

    private CardItemBuilder(){
    }

    public static CardItemBuilder create(){
        return new CardItemBuilder();
    }

    public CardItemBuilder setCount(int count){
        this.count = Math.max(count,1);
        return  this;
    }
}
