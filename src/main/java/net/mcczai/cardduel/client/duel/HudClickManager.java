package net.mcczai.cardduel.client.duel;

import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.block.entity.DuelTableBlockEntity;
import net.mcczai.cardduel.client.hud.DuelHandHud;
import net.mcczai.cardduel.duel.DuelPlayerData;
import net.mcczai.cardduel.items.ICard;
import net.mcczai.cardduel.network.payload.ClientboundDuelSyncPayload;
import net.mcczai.cardduel.network.payload.ServerboundAttackPayload;
import net.mcczai.cardduel.network.payload.ServerboundEndTurnPayload;
import net.mcczai.cardduel.network.payload.ServerboundMulliganPayload;
import net.mcczai.cardduel.network.payload.ServerboundPlayCardPayload;
import net.mcczai.cardduel.network.payload.ServerboundSurrenderPayload;
import net.mcczai.cardduel.resources.DefaultAssets;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * 无 Screen 的 HUD 点击命中层：
 *  - 右下按钮：PLAYING = 结束回合；MULLIGAN = 确认换牌
 *  - 手牌点击：出牌选中 / 换牌选中
 *  - 桌面射线命中：己方空槽出牌、己方有卡槽选中攻击、对方目标攻击/打脸
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = CardduelMod.MODID, value = Dist.CLIENT)
public class HudClickManager {

    private static final int BTN_W = 80;
    private static final int BTN_H = 20;
    private static final int BTN_MARGIN = 8;

    /** 桌面战场槽位几何（与 DuelTableBlockEntityRenderer 保持一致） */
    private static final float BOARD_STEP = (2.6F + 0.3F) / 16F;

    public static Rect2i endTurnButtonRect(int screenW, int screenH) {
        return new Rect2i(screenW - BTN_W - BTN_MARGIN, screenH - BTN_H - BTN_MARGIN, BTN_W, BTN_H);
    }

    /** 认输按钮：右下、"结束回合/确认换牌"按钮的正上方 */
    public static Rect2i surrenderButtonRect(int screenW, int screenH) {
        return new Rect2i(screenW - BTN_W - BTN_MARGIN, screenH - BTN_H - BTN_MARGIN - BTN_H - 4, BTN_W, BTN_H);
    }

    /**
     * 右下按钮：PLAYING 显示"结束回合"（非己方回合置灰）；MULLIGAN 显示"确认换牌"（已确认后置灰并提示等待对手）。
     */
    public static void renderEndTurnButton(GuiGraphics g, boolean myTurn, boolean mulliganPhase) {
        Minecraft mc = Minecraft.getInstance();
        Rect2i rect = endTurnButtonRect(g.guiWidth(), g.guiHeight());
        boolean waiting = mulliganPhase && DuelInteraction.isMulliganDone();
        int color = waiting ? 0xFF616161 : (mulliganPhase ? 0xFFF9A825 : (myTurn ? 0xFF43A047 : 0xFF616161));
        g.fill(rect.getX(), rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), color);
        Component label = waiting
                ? Component.translatable("cardduel.hud.mulligan_wait")
                : (mulliganPhase
                        ? Component.translatable("cardduel.hud.mulligan_confirm")
                        : Component.translatable("cardduel.hud.end_turn"));
        g.drawCenteredString(mc.font, label, rect.getX() + rect.getWidth() / 2, rect.getY() + (BTN_H - 8) / 2, 0xFFFFFFFF);
    }

    /**
     * 认输按钮（换牌阶段与对局中显示）。
     */
    public static void renderSurrenderButton(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Rect2i rect = surrenderButtonRect(g.guiWidth(), g.guiHeight());
        g.fill(rect.getX(), rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), 0xFFC62828);
        g.drawCenteredString(mc.font, Component.translatable("cardduel.hud.surrender"),
                rect.getX() + rect.getWidth() / 2, rect.getY() + (BTN_H - 8) / 2, 0xFFFFFFFF);
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != 0 || event.getAction() != 1) {
            return; // 仅处理鼠标左键按下
        }
        Minecraft mc = Minecraft.getInstance();
        if ((mc.screen != null && !(mc.screen instanceof DuelHudScreen)) || mc.player == null || mc.level == null) {
            return;
        }
        ClientboundDuelSyncPayload sync = ClientDuelState.get();
        if (sync == null || !DuelCameraManager.isActivePhase(sync.phase())) {
            return;
        }

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        double mx = mc.mouseHandler.xpos() * sw / (double) mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * sh / (double) mc.getWindow().getScreenHeight();
        boolean myTurn = mc.player.getUUID().equals(sync.activeUuid());
        boolean mulligan = "MULLIGAN".equals(sync.phase());

        // 1. 认输按钮（换牌与对局中都可认输）
        if (surrenderButtonRect(sw, sh).contains((int) mx, (int) my)) {
            DuelInteraction.clear();
            PacketDistributor.sendToServer(new ServerboundSurrenderPayload());
            event.setCanceled(true);
            return;
        }

        // 2. 右下按钮（结束回合 / 确认换牌）
        Rect2i btn = endTurnButtonRect(sw, sh);
        if (btn.contains((int) mx, (int) my)) {
            if (mulligan) {
                // 不做客户端拦截（重复确认由服务端 isMulliganDone 兜底去重）：
                // 一旦客户端状态残留（如上一局遗留的"已确认"），拦截会导致永远发不出确认包。
                PacketDistributor.sendToServer(new ServerboundMulliganPayload(
                        new ArrayList<>(DuelInteraction.getMulliganSelection())));
                DuelInteraction.clearMulligan();
                DuelInteraction.setMulliganDone(true);
            } else if (myTurn) {
                PacketDistributor.sendToServer(new ServerboundEndTurnPayload());
            }
            event.setCanceled(true);
            return;
        }

        // 3. 手牌
        int handIndex = DuelHandHud.handIndexAt(mx, my, ClientDuelHand.get().size(), sw, sh);
        if (handIndex >= 0) {
            if (mulligan) {
                // 已确认换牌后不再允许改选；硬币卡不可选（服务端同样会拒绝，这里直接忽略点击）
                if (!DuelInteraction.isMulliganDone()) {
                    ItemStack clicked = ClientDuelHand.get().get(handIndex);
                    ICard access = ICard.getICardOrNull(clicked);
                    boolean isCoin = access != null
                            && DefaultAssets.COIN_CARD_ID.equals(access.getCardId(clicked));
                    if (!isCoin) {
                        DuelInteraction.toggleMulligan(handIndex);
                    }
                }
            } else if (myTurn) {
                // 按下手牌进入拖拽：松开时拖到己方半场即打出，原地松开即按旧流程选中
                DuelInteraction.startDragHand(handIndex);
            }
            event.setCanceled(true);
            return;
        }

        // 3. 桌面射线
        SlotHit hit = hitTable(mc, sync, mx, my);
        if (hit != null) {
            handleTableClick(mc, sync, hit, myTurn, mulligan);
            event.setCanceled(true);
            return;
        }

        // 4. 空白处：清空选中
        DuelInteraction.clear();
    }

    /**
     * 拖拽结算：松开鼠标时按落点决定 打出 / 攻击 / 选中。
     * 落点在牌桌上复用 handleTableClick 的整套分支；落点不在桌上则回退为旧的点选流程。
     */
    @SubscribeEvent
    public static void onMouseRelease(InputEvent.MouseButton.Post event) {
        if (event.getButton() != 0 || event.getAction() != 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if ((mc.screen != null && !(mc.screen instanceof DuelHudScreen)) || mc.player == null || mc.level == null) {
            return;
        }
        ClientboundDuelSyncPayload sync = ClientDuelState.get();
        if (sync == null || !DuelCameraManager.isActivePhase(sync.phase())) {
            return;
        }
        if ("MULLIGAN".equals(sync.phase()) || !DuelInteraction.isDragging()) {
            return;
        }
        boolean myTurn = mc.player.getUUID().equals(sync.activeUuid());

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        double mx = mc.mouseHandler.xpos() * sw / (double) mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * sh / (double) mc.getWindow().getScreenHeight();

        int hand = DuelInteraction.getDraggingHand();
        int board = DuelInteraction.getDraggingBoard();
        SlotHit hit = hitTable(mc, sync, mx, my);
        boolean isHost = mc.player.getUUID().equals(sync.hostUuid());

        if (hand >= 0) {
            if (hit != null) {
                // 拖到牌桌：按卡牌类型走打出分支（召唤站场/魔法结算/装备附着/陷阱等）
                DuelInteraction.selectHand(hand);
                handleTableClick(mc, sync, hit, myTurn, false);
                DuelInteraction.clear();
            } else {
                // 原地松开：按旧的点选流程选中手牌
                DuelInteraction.toggleHand(hand);
            }
        } else if (board >= 0) {
            if (hit != null && hit.hostHalf() != isHost) {
                // 拖到对方半场：攻击（有卡目标打目标，点空处打脸）
                DuelInteraction.selectBoard(board);
                handleTableClick(mc, sync, hit, myTurn, false);
                DuelInteraction.clear();
            } else {
                // 原地/己方半场松开：选中召唤物（后续点对方目标攻击）
                DuelInteraction.toggleBoard(board);
            }
        }
        DuelInteraction.stopDrag();
    }

    /**
     * 选中手牌的类型（决定点击目标的分支）：
     * "mana" 魔法 / "equip" 装备 / "secret" 秘密陷阱 / "anvil" 铁砧陷阱 / "summon" 召唤及一切站场兜底。
     */
    public static String selectedHandKind() {
        int index = DuelInteraction.getSelectedHand();
        if (index < 0 || index >= ClientDuelHand.get().size()) {
            return null;
        }
        ItemStack card = ClientDuelHand.get().get(index);
        ICard access = ICard.getICardOrNull(card);
        if (access == null) {
            return "summon";
        }
        String type = access.getType(card);
        String skill = access.getSkill(card);
        if ("mana".equals(type)) {
            return "mana";
        }
        if ("equip".equals(type)) {
            return "equip";
        }
        if (skill != null && skill.startsWith("secret_")) {
            return "secret";
        }
        if (skill != null && skill.startsWith("anvil_")) {
            return "anvil";
        }
        return "summon"; // 含 trap thorns / 未知技能兜底
    }

    private static void handleTableClick(Minecraft mc, ClientboundDuelSyncPayload sync, SlotHit hit,
                                         boolean myTurn, boolean mulligan) {
        if (mulligan || !myTurn) {
            DuelInteraction.clear();
            return;
        }
        if (!(mc.level.getBlockEntity(sync.tablePos()) instanceof DuelTableBlockEntity table)) {
            return;
        }
        boolean isHost = mc.player.getUUID().equals(sync.hostUuid());
        DuelPlayerData myData = isHost ? table.getHostData() : table.getGuestData();
        DuelPlayerData foeData = isHost ? table.getGuestData() : table.getHostData();
        int selectedHand = DuelInteraction.getSelectedHand();
        String kind = selectedHandKind();

        if (hit.hostHalf() == isHost) {
            // 己方半场
            if (selectedHand >= 0 && kind != null) {
                switch (kind) {
                    case "mana", "secret" -> {
                        // 魔法/秘密：点己方半场任意处打出（不占槽）
                        PacketDistributor.sendToServer(new ServerboundPlayCardPayload(selectedHand, -1));
                        DuelInteraction.clear();
                        return;
                    }
                    case "equip" -> {
                        // 装备：点己方有卡槽附着
                        if (!myData.getBoard()[hit.slot()].isEmpty()) {
                            PacketDistributor.sendToServer(new ServerboundPlayCardPayload(selectedHand, hit.slot()));
                        }
                        DuelInteraction.clear();
                        return;
                    }
                    default -> {
                        // 召唤/反伤陷阱/未知：点己方空槽站场
                        if (myData.getBoard()[hit.slot()].isEmpty()) {
                            PacketDistributor.sendToServer(new ServerboundPlayCardPayload(selectedHand, hit.slot()));
                        }
                        DuelInteraction.clear();
                        return;
                    }
                }
            }
            if (myData.getBoard()[hit.slot()].isEmpty()) {
                DuelInteraction.clear();
            } else {
                // 按下己方召唤物：进入拖拽，松开时拖到对方半场即攻击，原地松开即选中
                DuelInteraction.startDragBoard(hit.slot());
            }
        } else {
            // 对方半场
            if (selectedHand >= 0 && "anvil".equals(kind)) {
                // 铁砧：点对方召唤物坠落
                if (!foeData.getBoard()[hit.slot()].isEmpty()) {
                    PacketDistributor.sendToServer(new ServerboundPlayCardPayload(selectedHand, hit.slot()));
                }
                DuelInteraction.clear();
                return;
            }
            // 有选中己方卡 → 攻击对方卡或打脸（点空处）
            if (DuelInteraction.getSelectedBoard() >= 0) {
                int target = foeData.getBoard()[hit.slot()].isEmpty() ? -1 : hit.slot();
                PacketDistributor.sendToServer(new ServerboundAttackPayload(
                        DuelInteraction.getSelectedBoard(), target));
            }
            DuelInteraction.clear();
        }
    }

    /**
     * 屏幕坐标 → 桌面平面（按当前真实相机射线求交）→ 半场与槽位。
     * 不依赖"俯视相机"假设：相机处于任意位置/角度都能正确命中桌面。
     */
    @Nullable
    public static SlotHit hitTable(Minecraft mc, ClientboundDuelSyncPayload sync, double mouseX, double mouseY) {
        Direction facing = Direction.byName(sync.facing());
        if (facing == null) {
            return null;
        }
        int dirZ = facing == Direction.NORTH ? -1 : 1;
        BlockPos bePos = sync.tablePos();
        double tableTopY = bePos.getY() + 1.0;

        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 pos = cam.getPosition();
        Vec3 look = new Vec3(cam.getLookVector());
        if (look.y >= -0.01 || pos.y <= tableTopY) {
            // 视线接近水平或相机低于桌面：不可能命中桌面
            return null;
        }

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        double fov = mc.options.fov().get();
        double tanY = Math.tan(Math.toRadians(fov / 2.0));
        double aspect = (double) sw / sh;
        double ndcX = mouseX / sw * 2 - 1;
        double ndcY = mouseY / sh * 2 - 1;

        Vec3 right = new Vec3(cam.getLeftVector()).scale(-1.0);
        Vec3 dir = look
                .add(right.scale(ndcX * tanY * aspect))
                .add(new Vec3(cam.getUpVector()).scale(ndcY * tanY))
                .normalize();

        double t = (tableTopY - pos.y) / dir.y;
        if (t < 0) {
            return null;
        }
        double localX = (pos.x + dir.x * t) - bePos.getX();
        double localZ = (pos.z + dir.z * t) - bePos.getZ();

        double hostZ = 0.5 + dirZ * 0.25;
        double guestZ = 0.5 + dirZ * 0.75;
        boolean hostHalf = Math.abs(localZ - hostZ) < Math.abs(localZ - guestZ);
        double halfZ = hostHalf ? hostZ : guestZ;
        if (Math.abs(localZ - halfZ) > 0.25) {
            return null;
        }

        float start = 6 * BOARD_STEP / 2F;
        int slot = -1;
        for (int i = 0; i < DuelPlayerData.BOARD_SIZE; i++) {
            double cx = 0.5 + (i * BOARD_STEP - start);
            if (Math.abs(localX - cx) <= BOARD_STEP / 2) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            return null;
        }
        return new SlotHit(hostHalf, slot);
    }

    public record SlotHit(boolean hostHalf, int slot) {
    }
}
