package net.mcczai.cardduel.item.builder;

import com.google.common.base.Preconditions;
import net.mcczai.cardduel.API.CdAPI;
import net.mcczai.cardduel.item.AbstractCardItem;
import net.mcczai.cardduel.item.CardItem;
import net.mcczai.cardduel.item.CardItemManager;
import net.mcczai.cardduel.item.ICard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class CardItemBuilder {
    private int count = 1;
    private int hp = 1;
    private int mp = 1;
    private int atk = 1;
    private int type = 0;
    private ResourceLocation cardId;
    private String skill;

    private CardItemBuilder(){

    }

    public static CardItemBuilder create(){
        return new CardItemBuilder();
    }

    public CardItemBuilder setCount(int count){
        this.count = Math.max(count,1);
        return this;
    }

    public CardItemBuilder setHP(int hp){
        this.hp = Math.max(hp,1);
        return this;
    }

    public CardItemBuilder setMP(int mp){
        this.mp = Math.max(mp,1);
        return this;
    }

    public CardItemBuilder setATK(int atk){
        this.atk = Math.max(atk,1);
        return this;
    }

    public CardItemBuilder setType(int type){
        this.type = Math.max(type,0);
        return this;
    }

    public CardItemBuilder setId(ResourceLocation id) {
        this.cardId = id;
        return this;
    }

    public CardItemBuilder setSkill(String skill){
        this.skill = skill;
        return this;
    }

    public ItemStack build(){
        String type = CdAPI.getCommonCardIndex(cardId).map(index -> index.getPojo().getType()).orElse(null);
        Preconditions.checkArgument(type != null, "Could not found card id: " + cardId);

        DeferredHolder<? extends AbstractCardItem, CardItem> cardItemDeferredHolder = CardItemManager.getCardItemRegistryObject(type);

        ItemStack card = new ItemStack(cardItemDeferredHolder.get(),this.count);
        if (card.getItem() instanceof ICard iCard){
            iCard.setCardId(card,this.cardId);
            iCard.setHP(card,this.hp);
            iCard.setMP(card,this.mp);
            iCard.setATK(card,this.atk);
            iCard.setType(card,this.type);
            iCard.setSkill(card,this.skill);
        }
        return card;
    }
}
