package net.mcczai.cardduel.network;

import net.mcczai.cardduel.block.entity.DuelTableBlockEntity;
import net.mcczai.cardduel.duel.DuelEngine;
import net.mcczai.cardduel.duel.DuelPhase;
import net.mcczai.cardduel.duel.DuelSeat;
import net.mcczai.cardduel.init.ModAttachments;
import net.mcczai.cardduel.network.payload.ServerboundAttackPayload;
import net.mcczai.cardduel.network.payload.ServerboundEndTurnPayload;
import net.mcczai.cardduel.network.payload.ServerboundLeavePayload;
import net.mcczai.cardduel.network.payload.ServerboundMulliganPayload;
import net.mcczai.cardduel.network.payload.ServerboundPlayCardPayload;
import net.mcczai.cardduel.network.payload.ServerboundSetupPayload;
import net.mcczai.cardduel.network.payload.ServerboundSurrenderPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 网络包注册与处理器（公共侧）。
 * 客户端侧处理见 client/event/ClientPayloadHandlers。
 */
public final class DuelNet {

    private DuelNet() {
    }

    /**
     * 服务端方向（客户端 → 服务端）的包注册，在 Mod 构造器中调用。
     */
    public static void registerServer(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(ServerboundSetupPayload.TYPE, ServerboundSetupPayload.STREAM_CODEC, DuelNet::handleSetup)
                .playToServer(ServerboundLeavePayload.TYPE, ServerboundLeavePayload.STREAM_CODEC, DuelNet::handleLeave)
                .playToServer(ServerboundEndTurnPayload.TYPE, ServerboundEndTurnPayload.STREAM_CODEC, DuelNet::handleEndTurn)
                .playToServer(ServerboundPlayCardPayload.TYPE, ServerboundPlayCardPayload.STREAM_CODEC, DuelNet::handlePlayCard)
                .playToServer(ServerboundAttackPayload.TYPE, ServerboundAttackPayload.STREAM_CODEC, DuelNet::handleAttack)
                .playToServer(ServerboundMulliganPayload.TYPE, ServerboundMulliganPayload.STREAM_CODEC, DuelNet::handleMulligan)
                .playToServer(ServerboundSurrenderPayload.TYPE, ServerboundSurrenderPayload.STREAM_CODEC, DuelNet::handleSurrender);
    }

    /**
     * 出牌。
     */
    private static void handlePlayCard(ServerboundPlayCardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> withTable(context, (player, table) ->
                DuelEngine.playCard(player, table, payload.handIndex(), payload.boardSlot())));
    }

    /**
     * 攻击。
     */
    private static void handleAttack(ServerboundAttackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> withTable(context, (player, table) ->
                DuelEngine.attack(player, table, payload.attackerSlot(), payload.targetSlot())));
    }

    /**
     * 换牌确认。
     */
    private static void handleMulligan(ServerboundMulliganPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> withTable(context, (player, table) ->
                DuelEngine.mulligan(player, table, payload.indices())));
    }

    /**
     * 认输。
     */
    private static void handleSurrender(ServerboundSurrenderPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> withTable(context, (player, table) ->
                DuelEngine.surrender(player, table)));
    }

    /**
     * 按玩家座位定位牌桌后执行操作。
     */
    private static void withTable(IPayloadContext context, java.util.function.BiConsumer<ServerPlayer, DuelTableBlockEntity> action) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        DuelSeat seat = serverPlayer.getData(ModAttachments.DUEL_SEAT.get());
        if (seat == null) {
            return;
        }
        if (serverPlayer.serverLevel().getBlockEntity(seat.tablePos()) instanceof DuelTableBlockEntity table) {
            action.accept(serverPlayer, table);
        }
    }

    /**
     * 结束回合（HUD 按钮 / 未来交互共用）。
     */
    private static void handleEndTurn(ServerboundEndTurnPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            DuelSeat seat = serverPlayer.getData(ModAttachments.DUEL_SEAT.get());
            if (seat == null) {
                return;
            }
            if (serverPlayer.serverLevel().getBlockEntity(seat.tablePos()) instanceof DuelTableBlockEntity table) {
                DuelEngine.endTurn(serverPlayer, table);
            }
        });
    }

    /**
     * 离座（设置界面"取消"按钮 / 未来的 HUD 离座按钮）。
     * 依据玩家座位 attachment 定位牌桌并复用 DuelEngine 的离座逻辑。
     */
    private static void handleLeave(ServerboundLeavePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            DuelSeat seat = serverPlayer.getData(ModAttachments.DUEL_SEAT.get());
            if (seat == null) {
                return;
            }
            if (serverPlayer.serverLevel().getBlockEntity(seat.tablePos()) instanceof DuelTableBlockEntity table) {
                DuelEngine.handleLeave(serverPlayer, table);
            } else {
                // 牌桌已不存在：直接清理残留座位
                serverPlayer.removeData(ModAttachments.DUEL_SEAT.get());
            }
        });
    }

    /**
     * 房主提交对局上限：校验座位/阶段后写入牌桌并进入 WAITING。
     */
    private static void handleSetup(ServerboundSetupPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            // 距离校验，防止跨世界/超远伪造
            if (serverPlayer.distanceToSqr(payload.tablePos().getCenter()) > 64.0D * 64.0D) {
                return;
            }
            if (serverPlayer.serverLevel().getBlockEntity(payload.tablePos()) instanceof DuelTableBlockEntity table
                    && table.isHost(serverPlayer)
                    && table.getPhase() == DuelPhase.SETUP) {
                table.setCaps(
                        Mth.clamp(payload.manaCap(), 1, 99),
                        Mth.clamp(payload.hpCap(), 1, 999)
                );
                table.setPhase(DuelPhase.WAITING);
                serverPlayer.displayClientMessage(
                        Component.translatable("cardduel.duel.caps_set", table.getManaCap(), table.getHpCap()),
                        false
                );
            }
        });
    }
}
