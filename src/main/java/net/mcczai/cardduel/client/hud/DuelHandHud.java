package net.mcczai.cardduel.client.hud;

import net.mcczai.cardduel.API.CdAPI;
import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.API.item.nbt.CardDataAccessor;
import net.mcczai.cardduel.client.duel.ClientDuelHand;
import net.mcczai.cardduel.client.duel.DuelCameraManager;
import net.mcczai.cardduel.client.duel.DuelHudScreen;
import net.mcczai.cardduel.client.duel.DuelInteraction;
import net.mcczai.cardduel.client.resource.ClientCardIndex;
import net.mcczai.cardduel.client.render.CardValueOverlay;
import net.mcczai.cardduel.resources.DefaultAssets;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;

/**
 * 炉石式手牌 HUD：对局中把手牌（数据源 = ClientboundDuelHandPayload）平铺在窗口下方。
 * 注册为"最高层"的 GUI 层（见 ClientModEvents.onRegisterGuiLayers），
 * 保证绘制在聊天栏等所有原版/第三方层之上，不会被遮挡。
 * 点击命中检测与 HudClickManager 共用本类的手牌布局（handIndexAt）。
 */
@OnlyIn(Dist.CLIENT)
public class DuelHandHud {
    public static final int CARD_W = 44;
    public static final int CARD_H = 62;
    public static final int GAP = 4;
    public static final int HOVER_SCALE = 4;
    /** 手牌顶部距窗口底部：置于窗口最下方，覆盖在聊天栏之上（顶层绘制） */
    public static final int HAND_Y = 78;

    public static void renderLayer(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if ((mc.screen != null && !(mc.screen instanceof DuelHudScreen)) || mc.player == null || mc.level == null) {
            return;
        }
        if (!DuelCameraManager.inDuelView()) {
            return;
        }
        List<ItemStack> hand = ClientDuelHand.get();
        if (hand.isEmpty()) {
            return;
        }

        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        double mouseX = mc.mouseHandler.xpos() * screenWidth / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * screenHeight / mc.getWindow().getScreenHeight();

        int totalWidth = hand.size() * CARD_W + (hand.size() - 1) * GAP;
        int startX = (screenWidth - totalWidth) / 2;
        int y = screenHeight - HAND_Y;

        int hovered = -1;
        for (int i = 0; i < hand.size(); i++) {
            int x = startX + i * (CARD_W + GAP);
            if (mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H) {
                hovered = i;
            }
        }

        for (int i = 0; i < hand.size(); i++) {
            if (i == hovered) {
                continue;
            }
            drawCard(guiGraphics, hand.get(i), startX + i * (CARD_W + GAP), y, CARD_W, CARD_H, highlightOf(i));
        }
        if (hovered >= 0) {
            int hx = startX + hovered * (CARD_W + GAP) - HOVER_SCALE;
            int hy = y - HOVER_SCALE;
            drawCard(guiGraphics, hand.get(hovered), hx, hy, CARD_W + HOVER_SCALE * 2, CARD_H + HOVER_SCALE * 2, highlightOf(hovered));
        }

        // 拖拽中的手牌跟随光标
        int drag = DuelInteraction.getDraggingHand();
        if (drag >= 0 && drag < hand.size()) {
            int dx = (int) mouseX - CARD_W / 2;
            int dy = (int) mouseY - CARD_H / 2;
            drawCard(guiGraphics, hand.get(drag), dx, dy, CARD_W, CARD_H, 1);
        }
    }

    /** 0=无高亮 1=出牌选中 2=换牌选中 */
    private static int highlightOf(int index) {
        if (DuelInteraction.getSelectedHand() == index) {
            return 1;
        }
        if (DuelInteraction.getMulliganSelection().contains(index)) {
            return 2;
        }
        return 0;
    }

    /**
     * 手牌命中检测（HudClickManager 共用）。
     */
    public static int handIndexAt(double mouseX, double mouseY, int handCount, int screenW, int screenH) {
        if (handCount <= 0) {
            return -1;
        }
        int totalWidth = handCount * CARD_W + (handCount - 1) * GAP;
        int startX = (screenW - totalWidth) / 2;
        int y = screenH - HAND_Y;
        if (mouseY < y - HOVER_SCALE || mouseY >= y + CARD_H + HOVER_SCALE) {
            return -1;
        }
        for (int i = 0; i < handCount; i++) {
            int x = startX + i * (CARD_W + GAP);
            if (mouseX >= x - HOVER_SCALE && mouseX < x + CARD_W + HOVER_SCALE) {
                return i;
            }
        }
        return -1;
    }

    private static void drawCard(GuiGraphics guiGraphics, ItemStack stack, int x, int y,
                                 int width, int height, int highlight) {
        // 硬币卡：无索引贴图，画金色圆角块
        if (stack.getItem() instanceof CardDataAccessor accessor
                && DefaultAssets.COIN_CARD_ID.equals(accessor.getCardId(stack))) {
            int border = switch (highlight) {
                case 1 -> 0xFFFFFFFF;
                case 2 -> 0xFFFFD54F;
                default -> 0xFFB8860B;
            };
            guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, border);
            guiGraphics.fill(x, y, x + width, y + height, 0xFFDAA520);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("cardduel.hud.coin"),
                    x + width / 2, y + height / 2 - 4, 0xFF5D4037);
            return;
        }

        ResourceLocation texture = null;
        if (stack.getItem() instanceof CardDataAccessor accessor) {
            Optional<ClientCardIndex> cardIndex = CdAPI.getClientCardIndex(accessor.getCardId(stack));
            texture = cardIndex.map(ClientCardIndex::getTexture).orElse(null);
        }
        if (texture == null) {
            return;
        }
        int border = switch (highlight) {
            case 1 -> 0xFFFFFFFF;
            case 2 -> 0xFFFFD54F;
            default -> 0xFF3A3A3A;
        };
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, border);
        guiGraphics.blit(texture, x, y, width, height, 0, 0, width, height, width, height);
        if (highlight == 0) {
            guiGraphics.fill(x, y, x + width, y + height, 0x66000000);
        }
        // 实时数值层（攻击/生命/费用，颜色反映增减）
        CardValueOverlay.drawOnHud(guiGraphics, stack, CardValueOverlay.layoutFor(stack), x, y, width, height);
    }
}
