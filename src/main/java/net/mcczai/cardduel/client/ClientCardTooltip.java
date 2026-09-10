package net.mcczai.cardduel.client;

import net.mcczai.cardduel.client.render.CardValueLayout;
import net.mcczai.cardduel.client.render.CardValueOverlay;
import net.mcczai.cardduel.items.CardTooltipPart;
import net.mcczai.cardduel.items.ICard;
import net.mcczai.cardduel.resources.CommonCardIndex;
import net.mcczai.cardduel.resources.pojo.CardDataPOJO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 卡牌 tooltip 的客户端实现，三行结构（卡名由原版自动按稀有度着色）：
 * <ol>
 *   <li>派系｜技能 —— {@code data.tribe} + {@code data.description}</li>
 *   <li>介绍 —— {@code index.tooltip}（自动剥掉"派系 · 稀有度｜"前缀）</li>
 *   <li>属性 —— {@code ⚔攻 ❤血 ◆费} 图标行</li>
 * </ol>
 * 三段分别受 HideFlags 的 TYPE_INFO / DESCRIPTION / ATK_INFO+HP_INFO+MP_INFO 控制。
 */
public class ClientCardTooltip implements ClientTooltipComponent {

    /** 每段的换行宽度上限 */
    private static final int SECTION_MAX_WIDTH = 200;
    /** 每段最多显示行数 */
    private static final int SECTION_MAX_LINES = 2;

    private static final int COLOR_TRIBE = 0xAAAAAA;
    private static final int COLOR_SKILL = 0xFFFFFF;
    private static final int COLOR_FLAVOR = 0xAAAAAA;
    private static final String SEPARATOR = "｜";

    private final ItemStack card;
    private final ICard iCard;
    private final CommonCardIndex cardIndex;

    /** ① 派系｜技能 */
    private @Nullable List<FormattedCharSequence> headLines;
    /** ② 介绍（风味文本） */
    private @Nullable List<FormattedCharSequence> descLines;
    /** ③ 属性（分段着色） */
    private List<CardValueOverlay.Segment> valueSegments = List.of();

    private int maxWidth;
    private int height;

    public ClientCardTooltip(CardTooltip tooltip) {
        this.card = tooltip.card();
        this.iCard = tooltip.iCard();
        this.cardIndex = tooltip.cardIndex();
        this.getText();
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth(Font font) {
        return maxWidth;
    }

    @Override
    public void renderText(Font font, int mouseX, int mouseY, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
        float y = drawLines(font, this.headLines, mouseX, mouseY, matrix, bufferSource);
        y = drawLines(font, this.descLines, mouseX, y, matrix, bufferSource);

        if (!this.valueSegments.isEmpty()) {
            float x = mouseX;
            for (CardValueOverlay.Segment segment : this.valueSegments) {
                // drawInBatch 返回的是"绘制后新的 x"（Font.java:204），不能累加
                x = font.drawInBatch(segment.text(), x, y, segment.color(), true, matrix, bufferSource,
                        Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
        }
    }

    private static float drawLines(Font font, @Nullable List<FormattedCharSequence> lines,
                                   float x, float y, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
        if (lines == null) {
            return y;
        }
        for (FormattedCharSequence line : lines) {
            font.drawInBatch(line, x, y, -1, true, matrix, bufferSource,
                    Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            y += font.lineHeight;
        }
        return y;
    }

    private void getText() {
        Font font = Minecraft.getInstance().font;
        CardDataPOJO data = cardIndex.getCardData();

        // ① 派系｜技能
        if (shouldShow(CardTooltipPart.TYPE_INFO)) {
            MutableComponent head = Component.empty();
            String tribe = data != null ? data.getTRIBE() : null;
            String skill = data != null ? data.getDESCRIPTION() : null;
            if (tribe != null && !tribe.isBlank()) {
                head.append(Component.translatable("cardduel.tribe." + tribe).withStyle(styleOf(COLOR_TRIBE)))
                        .append(Component.literal(SEPARATOR).withStyle(styleOf(COLOR_TRIBE)));
            }
            if (skill != null && !skill.isBlank()) {
                head.append(Component.translatable(skill).withStyle(styleOf(COLOR_SKILL)));
            }
            if (!head.getString().isBlank()) {
                this.headLines = limitLines(font.split(head, SECTION_MAX_WIDTH));
            }
        }

        // ② 介绍：卡包文本可能带 "派系 · 稀有度｜" 前缀（生成管线可能再次写入），此处只取分隔符之后的部分
        if (shouldShow(CardTooltipPart.DESCRIPTION)) {
            String tooltipKey = cardIndex.getPojo().getTooltip();
            if (tooltipKey != null && !tooltipKey.isBlank()) {
                String flavor = stripPrefix(Component.translatable(tooltipKey).getString());
                if (!flavor.isBlank()) {
                    this.descLines = limitLines(font.split(
                            Component.literal(flavor).withStyle(styleOf(COLOR_FLAVOR)), SECTION_MAX_WIDTH));
                }
            }
        }

        // ③ 属性（⚔攻 ❤血 ◆费）
        CardValueLayout layout = CardValueOverlay.layoutFor(card);
        this.valueSegments = CardValueOverlay.tooltipSegments(card, layout);

        int lines = size(headLines) + size(descLines) + (valueSegments.isEmpty() ? 0 : 1);
        int width = 0;
        width = maxLineWidth(font, headLines, width);
        width = maxLineWidth(font, descLines, width);
        if (!valueSegments.isEmpty()) {
            int valueWidth = 0;
            for (CardValueOverlay.Segment segment : valueSegments) {
                valueWidth += font.width(segment.text());
            }
            width = Math.max(width, valueWidth);
        }

        this.maxWidth = width;
        this.height = lines == 0 ? 0 : lines * font.lineHeight + 2;
    }

    private static int maxLineWidth(Font font, @Nullable List<FormattedCharSequence> lines, int current) {
        int width = current;
        if (lines != null) {
            for (FormattedCharSequence line : lines) {
                width = Math.max(width, font.width(line));
            }
        }
        return width;
    }

    private static int size(@Nullable List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static List<FormattedCharSequence> limitLines(List<FormattedCharSequence> lines) {
        return lines.size() > SECTION_MAX_LINES ? lines.subList(0, SECTION_MAX_LINES) : lines;
    }

    /** 剥掉 "派系 · 稀有度｜" 前缀（中英分隔符都兼容），无分隔符则原样返回 */
    private static String stripPrefix(String text) {
        int index = text.lastIndexOf('｜');
        if (index < 0) {
            index = text.lastIndexOf('|');
        }
        return index >= 0 ? text.substring(index + 1).trim() : text.trim();
    }

    private static Style styleOf(int rgb) {
        return Style.EMPTY.withColor(rgb);
    }

    private boolean shouldShow(CardTooltipPart part) {
        return (CardTooltipPart.getHideFlags(this.card) & part.getMask()) == 0;
    }
}
