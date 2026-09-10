package net.mcczai.cardduel.duel;

import net.mcczai.cardduel.API.CdAPI;
import net.mcczai.cardduel.API.item.nbt.CardDataAccessor;
import net.mcczai.cardduel.block.entity.DuelTableBlockEntity;
import net.mcczai.cardduel.config.DuelConfig;
import net.mcczai.cardduel.event.DuelSeatLock;
import net.mcczai.cardduel.init.ModAttachments;
import net.mcczai.cardduel.init.ModItem;
import net.mcczai.cardduel.items.ICard;
import net.mcczai.cardduel.items.builder.CardItemBuilder;
import net.mcczai.cardduel.network.payload.ClientboundDuelHandPayload;
import net.mcczai.cardduel.network.payload.ClientboundDuelSyncPayload;
import net.mcczai.cardduel.network.payload.ClientboundDuelTrapPayload;
import net.mcczai.cardduel.network.payload.ClientboundOpenSetupPayload;
import net.mcczai.cardduel.resources.DefaultAssets;
import net.mcczai.cardduel.resources.pojo.CardDataPOJO;
import net.mcczai.cardduel.skill.SkillHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 对局核心逻辑（服务端权威）。
 * 所有对局状态变更都经由此类；P2 技能系统的触发点见 SkillHooks。
 */
public final class DuelEngine {

    private DuelEngine() {
    }

    // ==================== 入座 / 离座 ====================

    /**
     * 空手右键牌桌：入座 / 打开设置界面 / 触发开局。
     */
    public static InteractionResult handleTableUse(ServerPlayer player, DuelTableBlockEntity table) {
        if (!table.isDoubleTable()) {
            player.displayClientMessage(Component.translatable("cardduel.duel.need_double"), false);
            return InteractionResult.SUCCESS;
        }

        DuelSeat seat = player.getData(ModAttachments.DUEL_SEAT.get());
        if (seat != null && !seat.tablePos().equals(table.getBlockPos())) {
            player.displayClientMessage(Component.translatable("cardduel.duel.already_seated"), false);
            return InteractionResult.SUCCESS;
        }

        if (table.isHost(player)) {
            switch (table.getPhase()) {
                case SETUP ->
                        PacketDistributor.sendToPlayer(player, new ClientboundOpenSetupPayload(table.getBlockPos()));
                case WAITING -> tryStart(player, table);
                default -> {
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (table.isGuest(player)) {
            player.displayClientMessage(Component.translatable("cardduel.duel.already_guest"), false);
            return InteractionResult.SUCCESS;
        }

        switch (table.getPhase()) {
            case IDLE -> {
                table.setHost(player.getUUID());
                table.setPhase(DuelPhase.SETUP);
                player.setData(ModAttachments.DUEL_SEAT.get(), new DuelSeat(table.getBlockPos(), true));
                PacketDistributor.sendToPlayer(player, new ClientboundOpenSetupPayload(table.getBlockPos()));
                player.displayClientMessage(Component.translatable("cardduel.duel.host_seated"), false);
            }
            case SETUP, WAITING -> {
                if (table.getGuestUuid() != null) {
                    player.displayClientMessage(Component.translatable("cardduel.duel.table_full"), false);
                } else {
                    table.setGuest(player.getUUID());
                    player.setData(ModAttachments.DUEL_SEAT.get(), new DuelSeat(table.getBlockPos(), false));
                    player.displayClientMessage(Component.translatable("cardduel.duel.guest_seated"), false);
                }
            }
            default -> {
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 潜行右键牌桌：离座 / 取消提交。
     * 对局进行中（MULLIGAN/PLAYING）不允许离座。
     */
    public static InteractionResult handleLeave(ServerPlayer player, DuelTableBlockEntity table) {
        if (!table.isHost(player) && !table.isGuest(player)) {
            // 未在本桌入座：静默返回，避免刷屏；仅在坐于别桌时提示
            if (player.getData(ModAttachments.DUEL_SEAT.get()) != null) {
                player.displayClientMessage(Component.translatable("cardduel.duel.already_seated"), false);
            }
            return InteractionResult.SUCCESS;
        }

        DuelPhase phase = table.getPhase();
        if (phase == DuelPhase.MULLIGAN || phase == DuelPhase.PLAYING) {
            player.displayClientMessage(Component.translatable("cardduel.duel.cant_leave"), false);
            return InteractionResult.SUCCESS;
        }

        if (table.isHost(player)) {
            // 房主离场：清掉客人座位并整桌重置
            table.clearGuestSeat(player.getServer());
            table.resetTable();
        } else if (table.isGuest(player)) {
            // 先取消牌组（此时仍是客人身份，getDataFor 才能命中），再清空座位
            table.cancelDeck(player);
            table.setGuest(null);
        }

        player.removeData(ModAttachments.DUEL_SEAT.get());
        player.displayClientMessage(Component.translatable("cardduel.duel.leave_ok"), false);
        syncToPlayers(table);
        return InteractionResult.SUCCESS;
    }

    /**
     * 掉线处理：对局中判负；等待阶段按离座处理。
     */
    public static void handleDisconnect(ServerPlayer player, DuelTableBlockEntity table) {
        DuelPhase phase = table.getPhase();
        if (phase == DuelPhase.MULLIGAN || phase == DuelPhase.PLAYING) {
            UUID winnerUuid = table.isHost(player) ? table.getGuestUuid() : table.getHostUuid();
            broadcast(table, Component.translatable("cardduel.duel.disconnect", player.getGameProfile().getName()));
            if (winnerUuid != null) {
                broadcast(table, Component.translatable("cardduel.duel.finish", playerName(table, winnerUuid)));
            }
            player.removeData(ModAttachments.DUEL_SEAT.get());
            if (table.isHost(player)) {
                // 房主掉线：整桌解散（客人座位一并清理，回到 IDLE）
                table.clearGuestSeat(player.getServer());
                table.resetTable();
            } else {
                // 客人掉线：判房主胜，清空客人座位并回到 WAITING（房主可等新挑战者再开一局）
                table.setGuest(null);
                table.resetDuel();
            }
            syncToPlayers(table);
        } else {
            handleLeave(player, table);
        }
    }

    // ==================== 开局 ====================

    /**
     * 房主触发开局：校验 → 洗牌 → 起手 4 张 → 随机先手 → PLAYING。
     */
    public static boolean tryStart(ServerPlayer host, DuelTableBlockEntity table) {
        if (!table.isHost(host) || table.getPhase() != DuelPhase.WAITING) {
            return false;
        }
        if (!table.isDoubleTable()) {
            host.displayClientMessage(Component.translatable("cardduel.duel.need_double"), false);
            return false;
        }
        UUID guestUuid = table.getGuestUuid();
        if (guestUuid == null) {
            host.displayClientMessage(Component.translatable("cardduel.duel.start_need_guest"), false);
            return false;
        }
        ServerPlayer guest = host.getServer().getPlayerList().getPlayer(guestUuid);
        if (guest == null) {
            host.displayClientMessage(Component.translatable("cardduel.duel.start_guest_offline"), false);
            return false;
        }

        DuelPlayerData hostData = table.getHostData();
        DuelPlayerData guestData = table.getGuestData();
        if (!hostData.isDeckReady() || !guestData.isDeckReady()) {
            host.displayClientMessage(Component.translatable("cardduel.duel.start_need_decks"), false);
            return false;
        }

        Collections.shuffle(hostData.getDeck());
        Collections.shuffle(guestData.getDeck());

        hostData.setMulliganDone(false);
        guestData.setMulliganDone(false);

        hostData.setHp(table.getHpCap());
        guestData.setHp(table.getHpCap());
        hostData.setTurnCount(0);
        guestData.setTurnCount(0);

        boolean hostFirst = host.getRandom().nextBoolean();
        UUID firstUuid = hostFirst ? host.getUUID() : guestUuid;
        table.setActiveUuid(firstUuid);
        table.setTurnNumber(0);

        // 起手 4 张
        for (int i = 0; i < 4; i++) {
            drawCard(table, hostData, host.getGameProfile().getName());
            drawCard(table, guestData, guest.getGameProfile().getName());
        }
        // 后手获得硬币卡
        DuelPlayerData secondData = hostFirst ? guestData : hostData;
        secondData.getHand().add(createCoin());

        String firstName = hostFirst ? host.getGameProfile().getName() : guest.getGameProfile().getName();
        table.setPhase(DuelPhase.MULLIGAN);

        // 开局时把双方传送到牌桌旁的座位：防止有人提交牌组后走远，开局后仍留在原地
        DuelSeatLock.teleportToSeat(host, table);
        DuelSeatLock.teleportToSeat(guest, table);

        broadcast(table, Component.translatable("cardduel.duel.start", firstName));
        broadcast(table, Component.translatable("cardduel.duel.mulligan_prompt"));
        syncToPlayers(table);
        return true;
    }

    /**
     * 后手硬币卡：使用后本回合 +1 法力并消失。
     */
    private static ItemStack createCoin() {
        ItemStack coin = new ItemStack(ModItem.CARD_ITEM.get());
        if (coin.getItem() instanceof CardDataAccessor accessor) {
            accessor.setCardId(coin, DefaultAssets.COIN_CARD_ID);
        }
        return coin;
    }

    // ==================== 回合 ====================

    /**
     * 当前行动方的新回合开始：回合数 +1 → MP 上限 = min(该玩家回合数, 封顶) 并回满 → 抽 1 张。
     */
    public static void startTurn(DuelTableBlockEntity table) {
        UUID activeUuid = table.getActiveUuid();
        if (activeUuid == null) {
            return;
        }
        DuelPlayerData active = activeData(table);
        DuelPlayerData foe = foeData(table, active);
        table.setTurnNumber(table.getTurnNumber() + 1);
        active.setTurnCount(active.getTurnCount() + 1);

        int mpMax = Math.min(active.getTurnCount(), table.getManaCap());
        active.setMpMax(mpMax);
        active.setMp(mpMax);

        SkillHooks.onTurnStart(table, active);
        // 对手秘密区：secret_sculk → 本回合跳过抽牌
        if (!checkSecretsOnTurnStart(table, active, foe)) {
            drawCard(table, active, playerName(table, activeUuid));
        }

        broadcast(table, Component.translatable("cardduel.duel.turn_start",
                table.getTurnNumber(), playerName(table, activeUuid), active.getMp(), active.getMpMax()));
        syncToPlayers(table);
    }

    /**
     * 结束当前回合并切换到对方。
     */
    public static boolean endTurn(ServerPlayer player, DuelTableBlockEntity table) {
        if (table.getPhase() != DuelPhase.PLAYING) {
            return false;
        }
        if (!player.getUUID().equals(table.getActiveUuid())) {
            player.displayClientMessage(Component.translatable("cardduel.duel.cmd.not_your_turn"), false);
            return false;
        }
        UUID otherUuid = table.isHost(player) ? table.getGuestUuid() : table.getHostUuid();
        if (otherUuid == null) {
            return false;
        }

        SkillHooks.onTurnEnd(table, activeData(table));
        // 回合上限保险丝：达到配置上限立即结束，按剩余血量判胜（相等平局）
        if (table.getTurnNumber() >= DuelConfig.MAX_TURN_LIMIT.get()) {
            broadcast(table, Component.translatable("cardduel.duel.turn_limit",
                    table.getTurnNumber(), DuelConfig.MAX_TURN_LIMIT.get()));
            finishDuel(table, decideByHp(table));
            return true;
        }
        table.setActiveUuid(otherUuid);
        startTurn(table);
        return true;
    }

    /**
     * 回合上限判定：剩余血量多者胜；相等返回 null（平局）。
     */
    @Nullable
    private static UUID decideByHp(DuelTableBlockEntity table) {
        int hostHp = table.getHostData().getHp();
        int guestHp = table.getGuestData().getHp();
        if (hostHp > guestHp) {
            return table.getHostUuid();
        }
        if (guestHp > hostHp) {
            return table.getGuestUuid();
        }
        return null;
    }

    // ==================== 抽牌 / 伤害 / 胜负 ====================

    /**
     * 抽 1 张牌：牌库空 → 疲劳扣血；手牌满 → 烧入弃牌堆。
     */
    public static void drawCard(DuelTableBlockEntity table, DuelPlayerData data, String playerName) {
        if (!data.getDeck().isEmpty()) {
            ItemStack card = data.getDeck().removeFirst();
            if (data.getHand().size() < DuelPlayerData.MAX_HAND) {
                data.getHand().add(card);
                broadcast(table, Component.translatable("cardduel.duel.draw", playerName, data.getDeck().size()));
            } else {
                data.getDiscard().add(card);
                broadcast(table, Component.translatable("cardduel.duel.hand_full_burn", playerName));
            }
            SkillHooks.onDraw(table, data, card);
        } else {
            data.setFatigue(data.getFatigue() + 1);
            broadcast(table, Component.translatable("cardduel.duel.fatigue",
                    playerName, data.getFatigue(), data.getFatigue()));
            dealPlayerDamage(table, data, data.getFatigue());
        }
    }

    /**
     * 对玩家造成伤害并判定胜负。不死图腾生效时抵挡致命伤害：生命置为 5（不超上限）并消耗图腾。
     */
    public static void dealPlayerDamage(DuelTableBlockEntity table, DuelPlayerData target, int amount) {
        if (target.getHp() <= 0) {
            return;
        }
        if (target.isTotemActive() && target.getHp() - amount <= 0) {
            target.setTotemActive(false);
            target.setHp(Math.min(5, table.getHpCap()));
            broadcast(table, Component.translatable("cardduel.duel.totem_saved", dataName(table, target)));
            syncToPlayers(table);
            return;
        }
        target.setHp(target.getHp() - amount);
        SkillHooks.onPlayerDamaged(table, target, amount);
        syncToPlayers(table);
        if (target.getHp() <= 0) {
            UUID winnerUuid = target == table.getHostData() ? table.getGuestUuid() : table.getHostUuid();
            finishDuel(table, winnerUuid);
        }
    }

    /**
     * 对局结束：宣布胜者，清场并回到 WAITING（保留座位与上限，可直接下一局）。
     */
    public static void finishDuel(DuelTableBlockEntity table, @Nullable UUID winnerUuid) {
        if (table.getPhase() != DuelPhase.PLAYING) {
            return;
        }
        if (winnerUuid == null) {
            broadcast(table, Component.translatable("cardduel.duel.draw_game"));
        } else {
            broadcast(table, Component.translatable("cardduel.duel.finish", playerName(table, winnerUuid)));
        }
        table.resetDuel();
        syncToPlayers(table);
    }

    /**
     * 认输（换牌阶段或对局中均可）：判对方获胜，清场并回到 WAITING（保留座位与上限，可直接下一局）。
     */
    public static void surrender(ServerPlayer player, DuelTableBlockEntity table) {
        DuelPhase phase = table.getPhase();
        if (phase != DuelPhase.MULLIGAN && phase != DuelPhase.PLAYING) {
            return;
        }
        UUID winnerUuid = table.isHost(player) ? table.getGuestUuid() : table.getHostUuid();
        broadcast(table, Component.translatable("cardduel.duel.surrender", player.getGameProfile().getName()));
        if (winnerUuid != null) {
            broadcast(table, Component.translatable("cardduel.duel.finish", playerName(table, winnerUuid)));
        }
        table.resetDuel();
        syncToPlayers(table);
    }

    /**
     * 牌组提交后检查双方是否都已就绪。
     */
    public static void notifyDeckReady(DuelTableBlockEntity table) {
        if (table.getPhase() == DuelPhase.WAITING
                && table.getHostUuid() != null
                && table.getGuestUuid() != null
                && table.getHostData().isDeckReady()
                && table.getGuestData().isDeckReady()) {
            broadcast(table, Component.translatable("cardduel.duel.both_ready"));
        }
    }

    // ==================== 出牌 / 攻击 ====================

    /**
     * 打出手牌：按卡牌类型走不同结算路径。
     *  - summon：占己方战场槽（召唤失调、可攻击）
     *  - mana：立即结算内建效果 → 进弃牌堆（不占槽，boardSlot 传 -1）
     *  - trap：secret_* → 秘密区；anvil_* → 立即对敌方召唤物伤害（boardSlot=敌方槽）；其余（thorns/未知）→ 占槽站场
     *  - equip：附着到己方召唤物（每槽 1 件，新装备替换旧装备）
     * 未知技能一律按召唤卡站场兜底（兼容旧卡）。
     */
    public static boolean playCard(ServerPlayer player, DuelTableBlockEntity table, int handIndex, int boardSlot) {
        if (table.getPhase() != DuelPhase.PLAYING) {
            return false;
        }
        if (!player.getUUID().equals(table.getActiveUuid())) {
            player.displayClientMessage(Component.translatable("cardduel.duel.cmd.not_your_turn"), false);
            return false;
        }
        DuelPlayerData data = activeData(table);
        if (handIndex < 0 || handIndex >= data.getHand().size()) {
            return false;
        }

        ItemStack card = data.getHand().remove(handIndex);
        ICard iCard = ICard.getICardOrNull(card);
        if (iCard == null) {
            data.getHand().add(handIndex, card);
            return false;
        }

        DuelPlayerData foe = foeData(table, data);

        // 硬币卡：本回合 +1 法力并消失（不占槽、不耗 MP）
        if (DefaultAssets.COIN_CARD_ID.equals(iCard.getCardId(card))) {
            data.setMp(Math.min(data.getMpMax(), data.getMp() + 1));
            checkSecretsOnPlay(table, data, foe, card, -1);
            table.sync();
            syncToPlayers(table);
            broadcast(table, Component.translatable("cardduel.duel.coin_used",
                    playerName(table, player.getUUID())));
            return true;
        }

        int cost = iCard.getMP(card);
        if (data.getMp() < cost) {
            data.getHand().add(handIndex, card);
            player.displayClientMessage(Component.translatable("cardduel.duel.not_enough_mp"), false);
            return false;
        }

        String type = iCard.getType(card);
        String skill = iCard.getSkill(card);
        boolean ok;
        switch (type) {
            case "mana" -> ok = playMana(table, data, foe, card, skill);
            case "trap" -> ok = playTrap(table, data, foe, card, skill, boardSlot, player);
            case "equip" -> ok = playEquip(table, data, card, boardSlot, player);
            default -> ok = placeOnBoard(table, data, card, boardSlot);
        }

        if (!ok) {
            data.getHand().add(handIndex, card);
            return false;
        }

        data.setMp(data.getMp() - cost);
        checkSecretsOnPlay(table, data, foe, card, findBoardSlot(data, card));
        table.sync();
        syncToPlayers(table);
        broadcast(table, Component.translatable("cardduel.duel.card_played",
                playerName(table, player.getUUID())));
        return true;
    }

    /**
     * 己方召唤卡攻击：targetSlot >= 0 打对方召唤卡（炉石式互伤），-1 打脸。
     * 装备机制：equip_elytra 冲锋（无视召唤失调）/ equip_trident 风怒（每回合两次）/
     * equip_ranged 远程（不受反击）/ equip_taunt 嘲讽（对方必须先攻击嘲讽单位）。
     * 装备耐久：召唤物受到任意伤害时 -1（统一在 applyCardDamage 内处理）。
     */
    public static boolean attack(ServerPlayer player, DuelTableBlockEntity table, int attackerSlot, int targetSlot) {
        if (table.getPhase() != DuelPhase.PLAYING) {
            return false;
        }
        if (!player.getUUID().equals(table.getActiveUuid())) {
            player.displayClientMessage(Component.translatable("cardduel.duel.cmd.not_your_turn"), false);
            return false;
        }
        DuelPlayerData attackerData = activeData(table);
        DuelPlayerData targetData = attackerData == table.getHostData() ? table.getGuestData() : table.getHostData();
        if (attackerSlot < 0 || attackerSlot >= DuelPlayerData.BOARD_SIZE) {
            return false;
        }
        ItemStack attackerCard = attackerData.getBoard()[attackerSlot];
        ICard attackerAccess = ICard.getICardOrNull(attackerCard);
        if (attackerAccess == null) {
            return false;
        }
        // 反伤类陷阱（仙人掌/岩浆块）不能主动攻击
        if (isThorns(attackerAccess.getSkill(attackerCard))) {
            player.displayClientMessage(Component.translatable("cardduel.duel.thorns_cant_attack"), false);
            return false;
        }
        if (targetSlot >= DuelPlayerData.BOARD_SIZE) {
            return false;
        }

        // 目标有效性前置校验（攻击未发生时不得消耗攻击次数）
        ItemStack targetCard = ItemStack.EMPTY;
        ICard targetAccess = null;
        if (targetSlot >= 0) {
            targetCard = targetData.getBoard()[targetSlot];
            targetAccess = ICard.getICardOrNull(targetCard);
            if (targetAccess == null) {
                return false;
            }
        }

        int turnCount = attackerData.getTurnCount();
        boolean charge = hasEquipSkill(attackerData, attackerSlot, "elytra");
        if (!charge && attackerData.getSummonTurn()[attackerSlot] == turnCount) {
            player.displayClientMessage(Component.translatable("cardduel.duel.summon_sickness"), false);
            return false;
        }
        int attackTurn = attackerData.getAttackTurn()[attackerSlot];
        boolean windfury = hasEquipSkill(attackerData, attackerSlot, "trident");
        if (attackTurn == turnCount || (attackTurn == turnCount + 1 && !windfury)) {
            player.displayClientMessage(Component.translatable("cardduel.duel.already_attacked"), false);
            return false;
        }

        // 嘲讽：对方有嘲讽单位时，只能攻击嘲讽单位（不可打脸）
        if (hasTaunt(targetData) && (targetSlot < 0 || !hasEquipSkill(targetData, targetSlot, "taunt"))) {
            player.displayClientMessage(Component.translatable("cardduel.duel.taunt_block"), false);
            return false;
        }

        // 有效攻击力 = 卡面攻击 + 装备加成
        int atk = attackerAccess.getATK(attackerCard) + equipAtk(attackerData, attackerSlot);
        // 攻击次数标记：风怒第一次攻击记 turnCount+1，第二次记 turnCount（回合结束后自然失效）
        attackerData.getAttackTurn()[attackerSlot] =
                (windfury && attackTurn != turnCount + 1) ? turnCount + 1 : turnCount;

        if (targetSlot < 0) {
            // 打脸
            dealPlayerDamage(table, targetData, atk);
        } else {
            boolean targetThorns = isThorns(targetAccess.getSkill(targetCard));
            SkillHooks.onAttack(table, attackerData, attackerSlot, attackerCard,
                    targetData, targetSlot, targetCard);
            // 主攻击（装备耐久磨损统一在 applyCardDamage 内按"受到伤害"处理）
            applyCardDamage(table, targetData, targetSlot, targetCard, atk);
            // 炉石式互伤（目标已死亡或攻击方为远程则无反击）
            if (!hasEquipSkill(attackerData, attackerSlot, "ranged")
                    && attackerData.getBoard()[attackerSlot] == attackerCard
                    && targetData.getBoard()[targetSlot] == targetCard) {
                int counterAtk = targetAccess.getATK(targetCard) + equipAtk(targetData, targetSlot);
                applyCardDamage(table, attackerData, attackerSlot, attackerCard, counterAtk);
            }
            // 反伤（接触伤害：无论目标是否死亡，攻击方都受 1 点伤害）
            if (targetThorns && attackerData.getBoard()[attackerSlot] == attackerCard) {
                broadcast(table, Component.translatable("cardduel.duel.thorns_reflect"));
                applyCardDamage(table, attackerData, attackerSlot, attackerCard, 1);
            }
        }
        // 防守方秘密区：secret_wither（对方攻击时，攻击方受 2 伤）
        checkSecretsOnAttack(table, attackerData, targetData, attackerCard, attackerSlot);

        table.sync();
        syncToPlayers(table);
        return true;
    }

    /**
     * 对战场卡牌造成伤害。任意伤害来源（攻击/反击/反伤/魔法/陷阱）都会：
     *  1. equip_guard 减伤：伤害 -1（减到 0 视为未受伤，不磨损装备）
     *  2. 若仍有伤害：装备耐久 -1；耐久归零 → 装备重置后进弃牌堆，召唤物恢复原有属性
     *  3. 本体扣血；本体 HP + 装备层 ≤ 0 → 清槽，卡与装备均重置数值后进弃牌堆
     */
    private static void applyCardDamage(DuelTableBlockEntity table, DuelPlayerData owner, int slot,
                                        ItemStack card, int damage) {
        ICard access = ICard.getICardOrNull(card);
        if (access == null || damage <= 0) {
            return;
        }
        // 装备减伤（equip_guard）：伤害 -1，减到 0 视为未受伤
        if (hasEquipSkill(owner, slot, "guard")) {
            damage = Math.max(0, damage - 1);
            if (damage <= 0) {
                return;
            }
        }
        // 受到伤害：装备耐久 -1（耐久归零 → 重置进弃牌堆）
        ItemStack equip = owner.getEquipped()[slot];
        ICard equipAccess = ICard.getICardOrNull(equip);
        if (equipAccess != null) {
            int left = equipAccess.getHP(equip) - 1;
            if (left <= 0) {
                owner.getEquipped()[slot] = ItemStack.EMPTY;
                owner.getDiscard().add(freshCard(equip));
                broadcast(table, Component.translatable("cardduel.duel.equip_broken"));
            } else {
                equipAccess.setHP(equip, left);
            }
        }
        int newHp = access.getHP(card) - damage;
        if (newHp + equipDurability(owner, slot) <= 0) {
            owner.getBoard()[slot] = ItemStack.EMPTY;
            owner.getSummonTurn()[slot] = 0;
            owner.getAttackTurn()[slot] = 0;
            ItemStack deadEquip = owner.getEquipped()[slot];
            if (!deadEquip.isEmpty()) {
                owner.getDiscard().add(freshCard(deadEquip));
                owner.getEquipped()[slot] = ItemStack.EMPTY;
            }
            owner.getDiscard().add(freshCard(card));
            SkillHooks.onDeath(table, owner, slot, card);
        } else {
            access.setHP(card, newHp);
        }
    }

    // ==================== 四类结算：魔法 / 陷阱 / 装备 ====================

    /**
     * 魔法卡：打出后立即结算内建效果并进弃牌堆（不占槽）。
     * 内建效果集（P1 最小集，P2 由 SkillRegistry 取代）：
     * heal_N 回血 / harm_N 打对方玩家 / fire_N 随机敌方召唤物 / lava_N 随机敌方单位 /
     * draw_N 抽牌 / mana_N 本回合加法力 / golden_heal 回5+抽1 / pearl_strike 随机召唤物受 atk 伤 /
     * totem_protect 免死一次。未知技能 → 无效果直接进弃牌堆。
     */
    private static boolean playMana(DuelTableBlockEntity table, DuelPlayerData data, DuelPlayerData foe,
                                    ItemStack card, String skill) {
        ICard access = ICard.getICardOrNull(card);
        data.getDiscard().add(card);
        String name = dataName(table, data);
        if (skill.startsWith("heal_")) {
            int amount = skillAmount(skill, "heal_");
            data.setHp(Math.min(table.getHpCap(), data.getHp() + amount));
            broadcast(table, Component.translatable("cardduel.duel.mana_heal", name, amount));
        } else if (skill.startsWith("harm_")) {
            int amount = skillAmount(skill, "harm_");
            dealPlayerDamage(table, foe, amount);
        } else if (skill.startsWith("fire_")) {
            int amount = skillAmount(skill, "fire_");
            damageRandomEnemySummon(table, foe, amount);
        } else if (skill.startsWith("lava_")) {
            int amount = skillAmount(skill, "lava_");
            damageRandomEnemyUnit(table, foe, amount);
        } else if (skill.startsWith("draw_")) {
            int amount = skillAmount(skill, "draw_");
            for (int i = 0; i < amount; i++) {
                drawCard(table, data, name);
            }
        } else if (skill.startsWith("mana_")) {
            int amount = skillAmount(skill, "mana_");
            data.setMp(Math.min(data.getMpMax(), data.getMp() + amount));
            broadcast(table, Component.translatable("cardduel.duel.mana_gain", name, amount));
        } else if ("golden_heal".equals(skill)) {
            data.setHp(Math.min(table.getHpCap(), data.getHp() + 5));
            drawCard(table, data, name);
            broadcast(table, Component.translatable("cardduel.duel.mana_heal", name, 5));
        } else if ("pearl_strike".equals(skill)) {
            int amount = access != null ? access.getATK(card) : 1;
            damageRandomEnemySummon(table, foe, amount);
        } else if ("totem_protect".equals(skill)) {
            data.setTotemActive(true);
            broadcast(table, Component.translatable("cardduel.duel.totem_set", name));
        } else {
            broadcast(table, Component.translatable("cardduel.duel.mana_used", name));
        }
        return true;
    }

    /**
     * 陷阱卡：secret_* → 秘密区；anvil_N → 立即对敌方召唤物 N 伤；其余（thorns/未知）→ 占槽站场。
     */
    private static boolean playTrap(DuelTableBlockEntity table, DuelPlayerData data, DuelPlayerData foe,
                                    ItemStack card, String skill, int boardSlot, ServerPlayer player) {
        if (skill.startsWith("secret_")) {
            int limit = DuelConfig.TRAP_ZONE_LIMIT.get();
            if (data.getTrapZone().size() >= limit) {
                player.displayClientMessage(Component.translatable("cardduel.duel.trap_full", limit), false);
                return false;
            }
            data.getTrapZone().add(card);
            player.displayClientMessage(Component.translatable("cardduel.duel.secret_placed"), false);
            return true;
        }
        if (skill.startsWith("anvil_")) {
            if (boardSlot < 0 || boardSlot >= DuelPlayerData.BOARD_SIZE || foe.getBoard()[boardSlot].isEmpty()) {
                player.displayClientMessage(Component.translatable("cardduel.duel.anvil_need_target"), false);
                return false;
            }
            int amount = skillAmount(skill, "anvil_");
            applyCardDamage(table, foe, boardSlot, foe.getBoard()[boardSlot], amount);
            data.getDiscard().add(card);
            broadcast(table, Component.translatable("cardduel.duel.anvil_dropped", amount));
            return true;
        }
        // thorns / 未知技能：占槽站场兜底
        return placeOnBoard(table, data, card, boardSlot);
    }

    /**
     * 装备卡：附着到己方召唤物（每槽 1 件，新装备替换旧装备，旧装备重置后进弃牌堆）。
     * 装备的 atk 提供攻击加成（attack 时累加），hp 作为耐久（每次受攻击 -1，归零损坏）。
     */
    private static boolean playEquip(DuelTableBlockEntity table, DuelPlayerData data, ItemStack card,
                                     int boardSlot, ServerPlayer player) {
        if (boardSlot < 0 || boardSlot >= DuelPlayerData.BOARD_SIZE || data.getBoard()[boardSlot].isEmpty()) {
            player.displayClientMessage(Component.translatable("cardduel.duel.equip_need_target"), false);
            return false;
        }
        ItemStack old = data.getEquipped()[boardSlot];
        if (!old.isEmpty()) {
            data.getDiscard().add(freshCard(old));
            broadcast(table, Component.translatable("cardduel.duel.equip_replaced",
                    playerName(table, player.getUUID())));
        }
        data.getEquipped()[boardSlot] = card;
        broadcast(table, Component.translatable("cardduel.duel.equip_attached",
                playerName(table, player.getUUID())));
        return true;
    }

    /**
     * 召唤类（及一切站场兜底）占槽。
     */
    private static boolean placeOnBoard(DuelTableBlockEntity table, DuelPlayerData data, ItemStack card,
                                        int boardSlot) {
        if (boardSlot < 0 || boardSlot >= DuelPlayerData.BOARD_SIZE || !data.getBoard()[boardSlot].isEmpty()) {
            return false;
        }
        data.getBoard()[boardSlot] = card;
        data.getSummonTurn()[boardSlot] = data.getTurnCount();
        SkillHooks.onSummoned(table, data, boardSlot, card);
        return true;
    }

    // ==================== 秘密区触发 ====================

    /**
     * 因 actor 打出牌而检查陷阱主人（trapOwner）的秘密区。
     * placedSlot 由引用查找：-1 = 未站场（魔法/秘密/硬币），≥0 = actor 刚放置的战场槽。
     */
    private static void checkSecretsOnPlay(DuelTableBlockEntity table, DuelPlayerData actor, DuelPlayerData trapOwner,
                                           ItemStack card, int placedSlot) {
        for (ItemStack trap : new ArrayList<>(trapOwner.getTrapZone())) {
            if (table.getPhase() != DuelPhase.PLAYING) {
                break;
            }
            ICard access = ICard.getICardOrNull(trap);
            if (access == null) {
                continue;
            }
            String skill = access.getSkill(trap);
            boolean triggered = false;
            if ("secret_mine".equals(skill)) {
                dealPlayerDamage(table, actor, 2);
                triggered = true;
            } else if ("secret_arrow".equals(skill) && placedSlot >= 0) {
                ItemStack placed = actor.getBoard()[placedSlot];
                // equip_elytra（鞘翅）：免疫陷阱伤害（陷阱仍被消耗）
                if (!placed.isEmpty() && !hasEquipSkill(actor, placedSlot, "elytra")) {
                    applyCardDamage(table, actor, placedSlot, placed, 3);
                }
                triggered = true;
            } else if ("secret_tnt".equals(skill) && countBoard(actor) >= 3) {
                damageAllCreatures(table, actor, 3);
                damageAllCreatures(table, trapOwner, 3);
                triggered = true;
            }
            if (triggered) {
                trapOwner.getTrapZone().remove(trap);
                trapOwner.getDiscard().add(trap);
                broadcast(table, Component.translatable("cardduel.duel.secret_triggered",
                        dataName(table, trapOwner)));
            }
        }
    }

    /**
     * 因 attacker 的召唤物发起攻击而检查防守方秘密区：secret_wither → 攻击方受 2 伤。
     */
    private static void checkSecretsOnAttack(DuelTableBlockEntity table, DuelPlayerData attacker,
                                             DuelPlayerData defender, ItemStack attackerCard, int attackerSlot) {
        for (ItemStack trap : new ArrayList<>(defender.getTrapZone())) {
            if (table.getPhase() != DuelPhase.PLAYING) {
                break;
            }
            ICard access = ICard.getICardOrNull(trap);
            if (access == null) {
                continue;
            }
            if ("secret_wither".equals(access.getSkill(trap))) {
                defender.getTrapZone().remove(trap);
                defender.getDiscard().add(trap);
                broadcast(table, Component.translatable("cardduel.duel.secret_triggered",
                        dataName(table, defender)));
                // equip_elytra（鞘翅）：免疫陷阱伤害（陷阱仍被消耗）
                if (attacker.getBoard()[attackerSlot] == attackerCard
                        && !hasEquipSkill(attacker, attackerSlot, "elytra")) {
                    applyCardDamage(table, attacker, attackerSlot, attackerCard, 2);
                }
            }
        }
    }

    /**
     * 回合开始时检查对手秘密区：secret_sculk → 本回合跳过抽牌。返回是否触发。
     */
    private static boolean checkSecretsOnTurnStart(DuelTableBlockEntity table, DuelPlayerData active,
                                                   DuelPlayerData foe) {
        for (ItemStack trap : new ArrayList<>(foe.getTrapZone())) {
            ICard access = ICard.getICardOrNull(trap);
            if (access == null) {
                continue;
            }
            if ("secret_sculk".equals(access.getSkill(trap))) {
                foe.getTrapZone().remove(trap);
                foe.getDiscard().add(trap);
                broadcast(table, Component.translatable("cardduel.duel.secret_triggered",
                        dataName(table, foe)));
                broadcast(table, Component.translatable("cardduel.duel.skip_draw", dataName(table, active)));
                return true;
            }
        }
        return false;
    }

    // ==================== 内建工具 ====================

    private static DuelPlayerData foeData(DuelTableBlockEntity table, DuelPlayerData data) {
        return data == table.getHostData() ? table.getGuestData() : table.getHostData();
    }

    private static String dataName(DuelTableBlockEntity table, DuelPlayerData data) {
        return playerName(table, data == table.getHostData() ? table.getHostUuid() : table.getGuestUuid());
    }

    private static int skillAmount(String skill, String prefix) {
        try {
            return Math.max(1, Integer.parseInt(skill.substring(prefix.length())));
        } catch (RuntimeException e) {
            return 1;
        }
    }

    private static boolean isThorns(String skill) {
        return skill != null && skill.contains("thorns");
    }

    private static int equipAtk(DuelPlayerData data, int slot) {
        ItemStack equip = data.getEquipped()[slot];
        ICard access = ICard.getICardOrNull(equip);
        return access != null ? access.getATK(equip) : 0;
    }

    private static String equipSkill(DuelPlayerData data, int slot) {
        ItemStack equip = data.getEquipped()[slot];
        ICard access = ICard.getICardOrNull(equip);
        return access != null ? access.getSkill(equip) : null;
    }

    private static boolean hasEquipSkill(DuelPlayerData data, int slot, String key) {
        String skill = equipSkill(data, slot);
        return skill != null && skill.contains(key);
    }

    /** 装备层：装备当前耐久（也是额外生命层，随磨损递减） */
    private static int equipDurability(DuelPlayerData data, int slot) {
        ItemStack equip = data.getEquipped()[slot];
        ICard access = ICard.getICardOrNull(equip);
        return access != null ? access.getHP(equip) : 0;
    }

    private static boolean hasTaunt(DuelPlayerData data) {
        for (int i = 0; i < DuelPlayerData.BOARD_SIZE; i++) {
            if (!data.getBoard()[i].isEmpty() && hasEquipSkill(data, i, "taunt")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从注册表重建全新卡牌（重置数值），用于进弃牌堆时恢复初始状态。
     * 注册表无此卡（如硬币）时原样复制兜底。
     */
    private static ItemStack freshCard(ItemStack card) {
        ICard access = ICard.getICardOrNull(card);
        if (access == null) {
            return card.copy();
        }
        return CdAPI.getCommonCardIndex(access.getCardId(card))
                .map(index -> {
                    CardDataPOJO data = index.getCardData();
                    return CardItemBuilder.create()
                            .setId(access.getCardId(card))
                            .setHP(data.getHP())
                            .setMP(data.getMP())
                            .setATK(data.getATK())
                            .setSkill(data.getSKILL())
                            .setType(data.getTYPE())
                            .setTribe(data.getTRIBE())
                            .build();
                })
                .orElseGet(card::copy);
    }

    private static int countBoard(DuelPlayerData data) {
        int count = 0;
        for (ItemStack stack : data.getBoard()) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 按引用查找 card 在战场上的槽位（刚放置的卡即同一引用），未站场返回 -1。
     */
    private static int findBoardSlot(DuelPlayerData data, ItemStack card) {
        ItemStack[] board = data.getBoard();
        for (int i = 0; i < board.length; i++) {
            if (board[i] == card) {
                return i;
            }
        }
        return -1;
    }

    private static void damageAllCreatures(DuelTableBlockEntity table, DuelPlayerData owner, int damage) {
        ItemStack[] board = owner.getBoard();
        for (int i = 0; i < board.length; i++) {
            // equip_elytra（鞘翅）：免疫陷阱伤害
            if (!board[i].isEmpty() && !hasEquipSkill(owner, i, "elytra")) {
                applyCardDamage(table, owner, i, board[i], damage);
            }
        }
    }

    private static void damageRandomEnemySummon(DuelTableBlockEntity table, DuelPlayerData foe, int amount) {
        List<Integer> slots = new ArrayList<>();
        ItemStack[] board = foe.getBoard();
        for (int i = 0; i < board.length; i++) {
            if (!board[i].isEmpty()) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            dealPlayerDamage(table, foe, amount); // 无召唤物时兜底打玩家
            return;
        }
        int slot = slots.get(table.getLevel().random.nextInt(slots.size()));
        applyCardDamage(table, foe, slot, board[slot], amount);
    }

    private static void damageRandomEnemyUnit(DuelTableBlockEntity table, DuelPlayerData foe, int amount) {
        List<Integer> slots = new ArrayList<>();
        ItemStack[] board = foe.getBoard();
        for (int i = 0; i < board.length; i++) {
            if (!board[i].isEmpty()) {
                slots.add(i);
            }
        }
        if (slots.isEmpty() || table.getLevel().random.nextBoolean()) {
            dealPlayerDamage(table, foe, amount);
        } else {
            int slot = slots.get(table.getLevel().random.nextInt(slots.size()));
            applyCardDamage(table, foe, slot, board[slot], amount);
        }
    }

    // ==================== 换牌 ====================

    /**
     * 换牌确认：把选中（≤2 张、不可含硬币）放回牌库底，抽等量新牌。
     */
    public static boolean mulligan(ServerPlayer player, DuelTableBlockEntity table, List<Integer> indices) {
        if (table.getPhase() != DuelPhase.MULLIGAN) {
            return false;
        }
        DuelPlayerData data = table.getDataFor(player);
        if (data == null || data.isMulliganDone()) {
            if (data != null) {
                player.displayClientMessage(Component.translatable("cardduel.duel.mulligan_already"), false);
            }
            return false;
        }
        Set<Integer> set = new HashSet<>(indices);
        if (set.size() != indices.size() || set.size() > 2) {
            return false;
        }
        for (int i : set) {
            if (i < 0 || i >= data.getHand().size()) {
                return false;
            }
            ItemStack card = data.getHand().get(i);
            ICard access = ICard.getICardOrNull(card);
            if (access != null && DefaultAssets.COIN_CARD_ID.equals(access.getCardId(card))) {
                player.displayClientMessage(Component.translatable("cardduel.duel.mulligan_coin"), false);
                return false; // 硬币不可换
            }
        }

        List<Integer> sorted = new ArrayList<>(set);
        sorted.sort(Collections.reverseOrder());
        for (int i : sorted) {
            data.getDeck().add(data.getHand().remove(i));
        }
        String name = playerName(table, player.getUUID());
        for (int i = 0; i < sorted.size(); i++) {
            drawCard(table, data, name);
        }

        data.setMulliganDone(true);
        // 反馈：告知本人已确认（换牌要双方都确认才进入对局，之前这里无任何提示）
        player.displayClientMessage(Component.translatable("cardduel.duel.mulligan_wait"), false);
        checkMulliganComplete(table);
        syncToPlayers(table);
        return true;
    }

    /**
     * 双方都完成换牌 → 进入 PLAYING 并开始先手回合。
     */
    private static void checkMulliganComplete(DuelTableBlockEntity table) {
        if (table.getHostData().isMulliganDone() && table.getGuestData().isMulliganDone()) {
            table.setPhase(DuelPhase.PLAYING);
            broadcast(table, Component.translatable("cardduel.duel.mulligan_done"));
            startTurn(table);
        }
    }

    // ==================== 同步与工具 ====================

    /**
     * 把对局公开状态推送给双方玩家（P1-2 HUD 消费），并把各自的手牌定向发给本人。
     */
    public static void syncToPlayers(DuelTableBlockEntity table) {
        if (!(table.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        ClientboundDuelSyncPayload payload = ClientboundDuelSyncPayload.of(table);
        UUID[] uuids = {table.getHostUuid(), table.getGuestUuid()};
        DuelPlayerData[] datas = {table.getHostData(), table.getGuestData()};
        for (int i = 0; i < uuids.length; i++) {
            UUID uuid = uuids[i];
            if (uuid == null) {
                continue;
            }
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                PacketDistributor.sendToPlayer(player, payload);
                PacketDistributor.sendToPlayer(player, new ClientboundDuelHandPayload(datas[i].getHand()));
                PacketDistributor.sendToPlayer(player,
                        new ClientboundDuelTrapPayload(new ArrayList<>(datas[i].getTrapZone())));
            }
        }
    }

    private static void broadcast(DuelTableBlockEntity table, Component message) {
        if (!(table.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        UUID[] uuids = {table.getHostUuid(), table.getGuestUuid()};
        for (UUID uuid : uuids) {
            if (uuid == null) {
                continue;
            }
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.displayClientMessage(message, false);
            }
        }
    }

    private static DuelPlayerData activeData(DuelTableBlockEntity table) {
        if (table.getActiveUuid() != null && table.getActiveUuid().equals(table.getHostUuid())) {
            return table.getHostData();
        }
        return table.getGuestData();
    }

    private static String playerName(DuelTableBlockEntity table, UUID uuid) {
        if (table.getLevel() instanceof ServerLevel serverLevel) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                return player.getGameProfile().getName();
            }
        }
        return "?";
    }
}
