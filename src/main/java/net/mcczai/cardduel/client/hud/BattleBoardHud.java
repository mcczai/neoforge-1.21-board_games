package net.mcczai.cardduel.client.hud;

import net.mcczai.cardduel.block.entity.DuelTableBlockEntity;
import net.mcczai.cardduel.client.duel.ClientDuelState;
import net.mcczai.cardduel.client.duel.DuelCameraManager;
import net.mcczai.cardduel.client.duel.DuelHudScreen;
import net.mcczai.cardduel.client.duel.DuelInteraction;
import net.mcczai.cardduel.client.duel.HudClickManager;
import net.mcczai.cardduel.duel.DuelPlayerData;
import net.mcczai.cardduel.network.payload.ClientboundDuelSyncPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 对局 HUD：屏幕上方双方状态条（HP/法力/牌库/疲劳/秘密区/装备/图腾）+ 中央回合指示 + 右下"结束回合/认输"按钮。
 * 注册为最高层 GUI 层（见 ClientModEvents），保证盖在 Jade/JEI 等第三方叠加层之上。
 */
@OnlyIn(Dist.CLIENT)
public class BattleBoardHud {

    private static final int BAR_W = 200;
    private static final int BAR_H = 64;
    private static final int MARGIN = 8;

    public static void renderLayer(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || (mc.screen != null && !(mc.screen instanceof DuelHudScreen))) {
            return;
        }
        ClientboundDuelSyncPayload sync = ClientDuelState.get();
        if (sync == null) {
            return;
        }
        // 换牌确认状态随阶段复位：只要不在换牌阶段（含对局间隙的 WAITING）就复位。
        // 必须放在活动阶段判断之前——否则对局结束回到 WAITING 后这段不执行，
        // 下一局会带着上一局的"已确认"状态开局（按钮直接显示等待且永远发不出确认包）。
        if (!"MULLIGAN".equals(sync.phase())) {
            DuelInteraction.setMulliganDone(false);
        }
        if (!DuelCameraManager.isActivePhase(sync.phase())) {
            return;
        }

        int sw = g.guiWidth();

        boolean isHost = mc.player.getUUID().equals(sync.hostUuid());
        ClientboundDuelSyncPayload.PlayerSyncView me = isHost ? sync.host() : sync.guest();
        ClientboundDuelSyncPayload.PlayerSyncView foe = isHost ? sync.guest() : sync.host();
        String meName = isHost ? sync.hostName() : sync.guestName();
        String foeName = isHost ? sync.guestName() : sync.hostName();
        boolean myTurn = mc.player.getUUID().equals(sync.activeUuid());
        boolean mulligan = "MULLIGAN".equals(sync.phase());

        // 装备数从本地牌桌实体读取（装备为公开信息，随方块实体同步）
        int meEquip = 0;
        int foeEquip = 0;
        if (mc.level != null && mc.level.getBlockEntity(sync.tablePos()) instanceof DuelTableBlockEntity table) {
            DuelPlayerData myData = isHost ? table.getHostData() : table.getGuestData();
            DuelPlayerData foeData = isHost ? table.getGuestData() : table.getHostData();
            meEquip = countEquipped(myData);
            foeEquip = countEquipped(foeData);
        }

        renderPlayerBar(g, MARGIN, MARGIN, meName, me, sync, myTurn, meEquip);
        renderPlayerBar(g, sw - BAR_W - MARGIN, MARGIN, foeName, foe, sync, !myTurn, foeEquip);

        String turn = Component.translatable("cardduel.hud.turn_total",
                sync.turnNumber(), sync.turnLimit()).getString();
        // 剩余回合 ≤5 时以警告色提示（回合上限保险丝即将触发）
        int turnColor = sync.turnLimit() - sync.turnNumber() <= 5 ? 0xFFFF7043 : 0xFFFFFFFF;
        g.drawCenteredString(mc.font, turn, sw / 2, MARGIN + 4, turnColor);

        // 选中 / 换牌提示
        if (mulligan) {
            String hint = DuelInteraction.isMulliganDone()
                    ? Component.translatable("cardduel.hud.mulligan_wait").getString()
                    : Component.translatable("cardduel.hud.mulligan_hint",
                            DuelInteraction.getMulliganSelection().size()).getString();
            g.drawCenteredString(mc.font, hint, sw / 2, MARGIN + 20, 0xFFFFD54F);
        } else if (DuelInteraction.getSelectedHand() >= 0) {
            String hint = switch (String.valueOf(HudClickManager.selectedHandKind())) {
                case "mana", "secret" -> Component.translatable("cardduel.hud.selected_mana").getString();
                case "anvil" -> Component.translatable("cardduel.hud.selected_anvil").getString();
                case "equip" -> Component.translatable("cardduel.hud.selected_equip").getString();
                default -> Component.translatable("cardduel.hud.selected_hand").getString();
            };
            g.drawCenteredString(mc.font, hint, sw / 2, MARGIN + 20, 0xFF81C784);
        } else if (DuelInteraction.getSelectedBoard() >= 0) {
            g.drawCenteredString(mc.font, Component.translatable("cardduel.hud.selected_board").getString(),
                    sw / 2, MARGIN + 20, 0xFFFFD54F);
        }

        HudClickManager.renderSurrenderButton(g);
        HudClickManager.renderEndTurnButton(g, myTurn, mulligan);
    }

    private static int countEquipped(DuelPlayerData data) {
        int count = 0;
        for (ItemStack stack : data.getEquipped()) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static void renderPlayerBar(GuiGraphics g, int x, int y, String name,
                                        ClientboundDuelSyncPayload.PlayerSyncView view,
                                        ClientboundDuelSyncPayload sync, boolean active, int equipCount) {
        Minecraft mc = Minecraft.getInstance();
        int border = active ? 0xFFFFD54F : 0xFF555555;
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, border);
        // 不透明背景：保证盖在第三方 HUD 上时仍可读
        g.fill(x, y, x + BAR_W, y + BAR_H, 0xF0202020);

        g.drawString(mc.font, name, x + 6, y + 4, 0xFFFFFFFF);
        g.drawString(mc.font,
                Component.translatable("cardduel.hud.hp", view.hp(), sync.hpCap()).getString(),
                x + 6, y + 16, 0xFFE57373);
        g.drawString(mc.font,
                Component.translatable("cardduel.hud.mp", view.mp(), view.mpMax()).getString(),
                x + 6, y + 28, 0xFF64B5F6);
        g.drawString(mc.font,
                Component.translatable("cardduel.hud.deck_fatigue", view.deckCount(), view.fatigue()).getString(),
                x + 6, y + 40, 0xFFBDBDBD);
        String extras = Component.translatable("cardduel.hud.trap_equip", view.trapCount(), equipCount).getString();
        if (view.totemActive()) {
            extras += " " + Component.translatable("cardduel.hud.totem").getString();
        }
        g.drawString(mc.font, extras, x + 6, y + 52, 0xFFCE93D8);
    }
}
