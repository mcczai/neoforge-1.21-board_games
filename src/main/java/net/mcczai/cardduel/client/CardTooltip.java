package net.mcczai.cardduel.client;

import net.mcczai.cardduel.API.CdAPI;
import net.mcczai.cardduel.items.ICard;
import net.mcczai.cardduel.resources.CommonCardIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

//用在客户端的Tooltip实现
public record CardTooltip(ItemStack card, ICard iCard, CommonCardIndex cardIndex) implements TooltipComponent {

    /**
     * 依据卡牌堆叠构造 tooltip 组件；非卡牌或无卡包索引（如硬币卡）返回空。
     */
    public static Optional<CardTooltip> of(ItemStack stack) {
        ICard iCard = ICard.getICardOrNull(stack);
        if (iCard == null) {
            return Optional.empty();
        }
        ResourceLocation cardId = iCard.getCardId(stack);
        return CdAPI.getCommonCardIndex(cardId).map(index -> new CardTooltip(stack, iCard, index));
    }
}
