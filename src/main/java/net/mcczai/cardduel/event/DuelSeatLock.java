package net.mcczai.cardduel.event;

import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.block.DuelTableBlock;
import net.mcczai.cardduel.block.entity.DuelTableBlockEntity;
import net.mcczai.cardduel.duel.DuelPhase;
import net.mcczai.cardduel.duel.DuelSeat;
import net.mcczai.cardduel.init.ModAttachments;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 对局进行中锁定玩家：
 *  - 每 tick 拉回座位锚点（防走远）
 *  - 屏蔽破坏/放置/使用物品/丢弃
 */
@EventBusSubscriber(modid = CardduelMod.MODID)
public class DuelSeatLock {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        DuelSeat seat = player.getData(ModAttachments.DUEL_SEAT.get());
        if (seat == null) {
            return;
        }
        if (player.serverLevel().getBlockEntity(seat.tablePos()) instanceof DuelTableBlockEntity table
                && isActiveDuel(table)) {
            Vec3 anchor = anchorFor(player, table);
            if (anchor != null && player.distanceToSqr(anchor) > 0.5D) {
                player.teleportTo(anchor.x, anchor.y, anchor.z);
                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 0.0F;
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (inActiveDuel(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (inActiveDuel(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (inActiveDuel(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (inActiveDuel(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    private static boolean inActiveDuel(Entity entity) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide) {
            return false;
        }
        DuelSeat seat = player.getData(ModAttachments.DUEL_SEAT.get());
        if (seat == null) {
            return false;
        }
        return player.serverLevel().getBlockEntity(seat.tablePos()) instanceof DuelTableBlockEntity table
                && isActiveDuel(table);
    }

    private static boolean isActiveDuel(DuelTableBlockEntity table) {
        return table.getPhase() == DuelPhase.PLAYING || table.getPhase() == DuelPhase.MULLIGAN;
    }

    /**
     * 立即把玩家传送到其座位锚点并面向牌桌（开局时调用一次；对局中的持续锁定见 onPlayerTick）。
     */
    public static void teleportToSeat(ServerPlayer player, DuelTableBlockEntity table) {
        Vec3 anchor = anchorFor(player, table);
        if (anchor == null) {
            return;
        }
        // 面朝牌桌中心（偏航角按世界坐标计算，微微俯视桌面）
        double dx = table.getBlockPos().getCenter().x - anchor.x;
        double dz = table.getBlockPos().getCenter().z - anchor.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        player.teleportTo(player.serverLevel(), anchor.x, anchor.y, anchor.z, yaw, 15.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }

    /**
     * 座位锚点：房主在双桌 facing 反向外侧，客人在 facing 外侧（离桌 1.5 米）。
     */
    private static Vec3 anchorFor(ServerPlayer player, DuelTableBlockEntity table) {
        Direction facing = table.getBlockState().getValue(DuelTableBlock.FACING);
        Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal());
        double offset = table.isHost(player) ? -1.5D : 1.5D;
        Vec3 center = table.getBlockPos().getCenter();
        return new Vec3(
                center.x + dir.x * offset,
                table.getBlockPos().getY(),
                center.z + dir.z * offset
        );
    }
}
