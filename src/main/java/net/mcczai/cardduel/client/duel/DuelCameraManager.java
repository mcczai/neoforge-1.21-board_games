package net.mcczai.cardduel.client.duel;

import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.network.payload.ClientboundDuelSyncPayload;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 对局中的俯视相机与界面裁剪：
 *  - 相机锁定双桌中心正上方，完全垂直俯视（90°）
 *  - 隐藏第一人称手部与所有原版 HUD 层（快捷栏/经验条/血量/饥饿/准星等）
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = CardduelMod.MODID, value = Dist.CLIENT)
public class DuelCameraManager {

    /** 相机离桌面高度（米） */
    public static final float CAMERA_HEIGHT = 6.0F;

    /** 一次性日志标记：确认相机改写与伪屏幕是否真正执行（排障用） */
    private static boolean cameraLogged;
    private static boolean screenLogged;
    private static boolean faceLogged;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // 不再限定阶段：任意阶段都强制俯视相机（消除阶段判断/事件时序的失败面）
        ClientboundDuelSyncPayload sync = ClientDuelState.get();
        if (sync == null || !isActivePhase(sync.phase())) {
            return;
        }
        Direction facing = Direction.byName(sync.facing());
        if (facing == null) {
            return;
        }
        // 双桌中心 = 主桌中心 + 朝向方向半步（双桌沿 facing 排列）
        Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 center = sync.tablePos().getCenter().add(dir.scale(0.5D));

        Camera camera = event.getCamera();
        camera.setPosition(center.x, center.y + CAMERA_HEIGHT, center.z);
        camera.setRotation(0.0F, 90.0F, 0.0F);

        if (!cameraLogged) {
            cameraLogged = true;
            CardduelMod.LOGGER.info("[cardduel] 俯视相机已接管：桌心=({}, {}, {}) 高度={} 阶段={} 事件阶段={}",
                    String.format("%.2f", center.x), String.format("%.2f", center.y),
                    String.format("%.2f", center.z), CAMERA_HEIGHT, sync.phase(), event.getStage());
        }
    }

    /**
     * 对局期间挂一个透明"伪屏幕"：释放鼠标（显示光标、停止转视角），
     * 世界与 HUD 照常渲染；对局结束（回到 WAITING 等阶段）自动关闭。
     * 若玩家自行打开了其它界面（如物品栏），不与其争抢，关闭后下个 tick 再挂回。
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean inView = inDuelView();
        if (inView) {
            if (mc.screen == null) {
                mc.setScreen(new DuelHudScreen());
                if (!screenLogged) {
                    screenLogged = true;
                    CardduelMod.LOGGER.info("[cardduel] 对局伪屏幕已打开（释放鼠标/停止转视角）");
                }
            }
            // 兜底：把本地玩家的视线强制转向牌桌（带俯角）。
            // 俯视相机在渲染阶段改写相机；这里同时修正玩家自身朝向，
            // 即使渲染相机被其他因素干扰，第一人称视角也始终朝向牌桌。
            faceTable(mc);
        } else if (mc.screen instanceof DuelHudScreen) {
            mc.setScreen(null);
            cameraLogged = false;
            screenLogged = false;
            faceLogged = false;
        }
    }

    /**
     * 让本地玩家看向牌桌桌面中心（俯视角度，兼容任何相机失效场景）。
     */
    private static void faceTable(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        ClientboundDuelSyncPayload sync = ClientDuelState.get();
        if (sync == null) {
            return;
        }
        Vec3 target = tableCenter(sync).add(0.0, 1.0, 0.0);
        Vec3 eye = mc.player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-4) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, horizontal));
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);

        if (!faceLogged) {
            faceLogged = true;
            CardduelMod.LOGGER.info("[cardduel] 玩家视线已转向牌桌：yaw={} pitch={} 目标=({}, {}, {})",
                    String.format("%.1f", yaw), String.format("%.1f", pitch),
                    String.format("%.2f", target.x), String.format("%.2f", target.y), String.format("%.2f", target.z));
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (inDuelView()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (inDuelView() && isVanillaLayerToHide(event.getName())) {
            event.setCanceled(true);
        }
    }

    private static boolean isVanillaLayerToHide(ResourceLocation name) {
        return name.equals(VanillaGuiLayers.HOTBAR)
                || name.equals(VanillaGuiLayers.EXPERIENCE_BAR)
                || name.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)
                || name.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || name.equals(VanillaGuiLayers.ARMOR_LEVEL)
                || name.equals(VanillaGuiLayers.FOOD_LEVEL)
                || name.equals(VanillaGuiLayers.AIR_LEVEL)
                || name.equals(VanillaGuiLayers.SELECTED_ITEM_NAME)
                || name.equals(VanillaGuiLayers.CROSSHAIR)
                || name.equals(VanillaGuiLayers.JUMP_METER);
    }

    /**
     * 是否处于对局视角（PLAYING / MULLIGAN）。
     */
    public static boolean inDuelView() {
        ClientboundDuelSyncPayload sync = ClientDuelState.get();
        return sync != null && isActivePhase(sync.phase());
    }

    public static boolean isActivePhase(String phase) {
        return "PLAYING".equals(phase) || "MULLIGAN".equals(phase);
    }

    /**
     * 双桌中心（俯视相机与座位锚点共用）。
     */
    public static Vec3 tableCenter(ClientboundDuelSyncPayload sync) {
        Direction facing = Direction.byName(sync.facing());
        if (facing == null) {
            return sync.tablePos().getCenter();
        }
        Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal());
        return sync.tablePos().getCenter().add(dir.scale(0.5D));
    }

    public static BlockPos tablePos(ClientboundDuelSyncPayload sync) {
        return sync.tablePos();
    }
}
