package net.mcczai.cardduel.client.duel;

import net.mcczai.cardduel.block.entity.DuelTableBlockEntity;
import net.mcczai.cardduel.client.CardTooltip;
import net.mcczai.cardduel.client.hud.DuelHandHud;
import net.mcczai.cardduel.client.renderer.blockentity.DuelTableBlockEntityRenderer;
import net.mcczai.cardduel.duel.DuelPhase;
import net.mcczai.cardduel.duel.DuelPlayerData;
import net.mcczai.cardduel.init.ModBlocks;
import net.mcczai.cardduel.network.payload.ClientboundDuelSyncPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 桌面/手牌卡牌悬停 tooltip：
 *  - 对局前（IDLE/SETUP/WAITING）：视线指向牌桌时，命中己方/对方的牌堆 → 显示牌组数量提示
 *  - 对局中（MULLIGAN/PLAYING）：手牌悬停 → 完整卡牌信息；桌面悬停 → 战场卡信息
 * 注册为最高层 GUI 层（见 ClientModEvents），保证画在手牌等所有层之上。
 */
@OnlyIn(Dist.CLIENT)
public class CardHoverTooltip {

    /** 悬停结果：战场单卡（pileCount=0）或准备阶段的牌堆（pileCount>0，card 为最上层那张） */
    private record HoverResult(ItemStack card, int pileCount) {
    }

    public static void renderLayer(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if ((mc.screen != null && !(mc.screen instanceof DuelHudScreen)) || mc.player == null || mc.level == null) {
            return;
        }

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        double mouseX = mc.mouseHandler.xpos() * sw / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * sh / mc.getWindow().getScreenHeight();

        HoverResult hovered = findHovered(mc, mouseX, mouseY, sw, sh);
        if (hovered == null || hovered.card().isEmpty()) {
            return;
        }

        if (hovered.pileCount() > 0) {
            // 牌堆：最上层卡名 + 数量提示
            guiGraphics.renderComponentTooltip(mc.font,
                    List.of(displayName(hovered.card()),
                            Component.translatable("cardduel.bag.pile_ready", hovered.pileCount())),
                    (int) mouseX, (int) mouseY);
            return;
        }

        Optional<CardTooltip> tooltip = CardTooltip.of(hovered.card());
        if (tooltip.isEmpty()) {
            return;
        }
        guiGraphics.renderTooltip(mc.font, List.of(displayName(hovered.card())), Optional.of(tooltip.get()),
                (int) mouseX, (int) mouseY);
    }

    /**
     * 卡名按原版稀有度着色：桌面/手牌 tooltip 是直接 renderTooltip 的，
     * 不会经过 ItemStack.getTooltipLines 的稀有度样式处理，这里手动补上。
     */
    private static Component displayName(ItemStack stack) {
        return stack.getHoverName().copy().withStyle(stack.getRarity().getStyleModifier());
    }

    /**
     * @return 悬停中的目标；没有则返回 null
     */
    @Nullable
    private static HoverResult findHovered(Minecraft mc, double mouseX, double mouseY, int screenW, int screenH) {
        ClientboundDuelSyncPayload sync = ClientDuelState.get();

        // 1. 对局中：先查手牌悬停（显示名称/派系|技能/介绍/属性），再查桌面战场
        if (sync != null && DuelCameraManager.isActivePhase(sync.phase())) {
            int handIndex = DuelHandHud.handIndexAt(mouseX, mouseY, ClientDuelHand.get().size(), screenW, screenH);
            if (handIndex >= 0) {
                ItemStack card = ClientDuelHand.get().get(handIndex);
                return card.isEmpty() ? null : new HoverResult(card, 0);
            }
            HudClickManager.SlotHit hit = HudClickManager.hitTable(mc, sync, mouseX, mouseY);
            if (hit == null) {
                return null;
            }
            if (!(mc.level.getBlockEntity(sync.tablePos()) instanceof DuelTableBlockEntity table)) {
                return null;
            }
            DuelPlayerData data = hit.hostHalf() ? table.getHostData() : table.getGuestData();
            ItemStack card = data.getBoard()[hit.slot()];
            return card.isEmpty() ? null : new HoverResult(card, 0);
        }

        // 2. 对局前：视线命中的牌桌 → 双方牌堆（准备阶段渲染为两叠）
        if (mc.hitResult instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK
                && mc.level.getBlockState(blockHit.getBlockPos()).is(ModBlocks.DUELTABLE_BLOCK.get())
                && mc.level.getBlockEntity(blockHit.getBlockPos()) instanceof DuelTableBlockEntity table) {
            DuelPhase phase = table.getPhase();
            if (phase != DuelPhase.IDLE && phase != DuelPhase.SETUP && phase != DuelPhase.WAITING) {
                return null;
            }
            int half = DuelTableBlockEntityRenderer.pileHalfAt(table,
                    blockHit.getLocation().x, blockHit.getLocation().z);
            if (half < 0) {
                return null;
            }
            DuelPlayerData data = half == 0 ? table.getHostData() : table.getGuestData();
            List<ItemStack> deck = data.getDeck();
            if (deck.isEmpty()) {
                return null;
            }
            return new HoverResult(deck.get(0), deck.size());
        }

        return null;
    }
}
