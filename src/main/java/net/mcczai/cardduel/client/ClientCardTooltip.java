package net.mcczai.cardduel.client;

import net.mcczai.cardduel.items.CardTooltipPart;
import net.mcczai.cardduel.items.ICard;
import net.mcczai.cardduel.resources.CommonCardIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.List;
    //用在客户端的Tooltip实现
public class ClientCardTooltip implements ClientTooltipComponent {

    private static final DecimalFormat FORMAT = new DecimalFormat("#.##%");
    private static final DecimalFormat DAMAGE_FORMAT = new DecimalFormat("#.##");

    private ItemStack card;
    private ICard iCard;
    private CommonCardIndex cardIndex;
    private @Nullable List<FormattedCharSequence> desc;
    
    private int maxWidth;

    public ClientCardTooltip(CardTooltip tooltip){
        this.card = tooltip.getCard();
        this.iCard = tooltip.getiCard();
        this.cardIndex = tooltip.getCardIndex();
        this.maxWidth = 0;
        this.getText();
    }



    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getWidth(Font font) {
        return maxWidth;
    }

    private void getText() {
        Font font = Minecraft.getInstance().font;

        if(shouldShow(CardTooltipPart.ATK_INFO)){
            @Nullable String tooltip = cardIndex.getPojo().getTooltip();
            if (tooltip != null){
                List<FormattedCharSequence> split = font.split(Component.translatable(tooltip),200);
                if (split.size() > 2){
                    this.desc = split.subList(0,2);
                }else {
                    this.desc = split;
                }
                for (FormattedCharSequence sequence : this.desc){
                    this.maxWidth = Math.max(font.width(sequence),this.maxWidth);
                }
            }
        }
    }

    private boolean shouldShow(CardTooltipPart part){
        return (CardTooltipPart.getHideFlags(this.card) & part.getMask()) == 0;
    }
}
