package net.mcczai.cardduel.client;

import net.mcczai.cardduel.items.ICard;
import net.mcczai.cardduel.resources.CommonCardIndex;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
    //用在客户端的Tooltip实现
public class CardTooltip implements TooltipComponent {
    private final ItemStack card;
    private final ICard iCard;
    private final CommonCardIndex cardIndex;

    public CardTooltip(ItemStack card, ICard iCard, CommonCardIndex cardIndex){
        this.card = card;
        this.iCard = iCard;
        this.cardIndex = cardIndex;
    }

    public ItemStack getCard() {
        return card;
    }

    public ICard getiCard() {
        return iCard;
    }

    public CommonCardIndex getCardIndex() {
        return cardIndex;
    }
}
