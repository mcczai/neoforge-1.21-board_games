package net.mcczai.cardduel.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mcczai.cardduel.API.CdAPI;
import net.mcczai.cardduel.API.item.nbt.CardDataAccessor;
import net.mcczai.cardduel.block.DuelTableBlock;
import net.mcczai.cardduel.block.entity.DuelTableBlockEntity;
import net.mcczai.cardduel.client.render.CardBacks;
import net.mcczai.cardduel.client.render.CardMesh;
import net.mcczai.cardduel.client.render.CardValueOverlay;
import net.mcczai.cardduel.client.resource.ClientCardIndex;
import net.mcczai.cardduel.duel.DuelPhase;
import net.mcczai.cardduel.duel.DuelPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 牌桌渲染（坐标系说明：双桌沿南北向排列，渲染原点 = 本方块实体所在桌）。
 *  - 对局前（IDLE/SETUP/WAITING）：双方已提交的牌组各摞成一叠，置于己方半场中央
 *  - 对局中（MULLIGAN/PLAYING）：双方战场 7+7 槽，各占半场，卡面朝上
 * 卡面 quad 经 rotateX(-90°) 后：本地 x → 世界 X；本地 y → 世界 -Z（北为负）。
 */
@OnlyIn(Dist.CLIENT)
public class DuelTableBlockEntityRenderer implements BlockEntityRenderer<DuelTableBlockEntity> {

    /** 卡面高度（桌面上方一点，避免 z-fighting） */
    private static final float CARD_Y = 1.01F;

    // 牌组展示：双方已提交的牌组各摞成一叠（视觉层数上限）——卡面贴图 48×64（3:4），长宽比必须一致
    private static final int PILE_LAYERS = 4;
    private static final float DECK_W = 2.4F / 16F;
    private static final float DECK_H = 3.2F / 16F;   // 2.4 : 3.2 = 3 : 4，与卡面贴图 48:64 对齐

    // 战场槽位卡（每方 7 张）——同样按 3:4 对齐贴图
    private static final int BOARD_SLOTS = 7;
    private static final float BOARD_W = 2.6F / 16F;
    private static final float BOARD_H = 3.4667F / 16F;  // 2.6 : 3.4667 = 3 : 4
    private static final float BOARD_GAP = 0.3F / 16F;

    public DuelTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DuelTableBlockEntity table, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        DuelPhase phase = table.getPhase();
        if (phase == DuelPhase.PLAYING || phase == DuelPhase.MULLIGAN) {
            renderBoard(table, poseStack, buffer, packedLight, packedOverlay);
        } else {
            renderDecks(table, poseStack, buffer, packedLight, packedOverlay);
            // 对局前：双方半场上空显示"谁已入座、是否已准备"
            renderStatusText(table, poseStack, buffer, packedLight);
        }
    }

    /**
     * 双桌中心沿 Z 轴的偏移（facing 指向配桌：北为 -Z，南为 +Z）。
     */
    private static float pairDirZ(DuelTableBlockEntity table) {
        Direction facing = table.getBlockState().getValue(DuelTableBlock.FACING);
        return facing == Direction.NORTH ? -1.0F : 1.0F;
    }

    // ==================== 对局前：牌组展示 ====================

    private void renderDecks(DuelTableBlockEntity table, PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        float dirZ = pairDirZ(table);
        float hostZ = 0.5F + dirZ * 0.25F;
        float guestZ = 0.5F + dirZ * 0.75F;
        // 双方已提交的牌组各自摞成一叠，置于己方半场中央
        renderPile(poseStack, buffer, table.getHostData().getDeck(), 0.5F, hostZ, packedLight, packedOverlay);
        renderPile(poseStack, buffer, table.getGuestData().getDeck(), 0.5F, guestZ, packedLight, packedOverlay);
    }

    /**
     * 把一方已提交的牌组画成一叠：最多 {@value #PILE_LAYERS} 层，底部层轻微错位营造厚度，
     * 最上层（牌组第一张）为可见牌面。
     */
    private void renderPile(PoseStack poseStack, MultiBufferSource buffer, List<ItemStack> deck,
                            float centerX, float centerZ, int packedLight, int packedOverlay) {
        if (deck.isEmpty()) {
            return;
        }
        int layers = Math.min(deck.size(), PILE_LAYERS);
        for (int i = 0; i < layers; i++) {
            ItemStack card = deck.get(i);
            ResourceLocation texture = cardTexture(card);
            if (texture == null) {
                continue;
            }
            float jitterX = (i % 2 == 0 ? 1F : -1F) * i * 0.002F;
            float jitterZ = (i % 2 == 1 ? 1F : -1F) * i * 0.002F;
            float lift = (layers - 1 - i) * (CardMesh.THICKNESS + 0.0004F);
            poseStack.pushPose();
            poseStack.translate(centerX - DECK_W / 2F + jitterX, CARD_Y + lift, centerZ + DECK_H / 2F + jitterZ);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
            poseStack.scale(DECK_W / CardMesh.TEX_W, DECK_H / CardMesh.TEX_H, 1F);
            CardMesh.renderSlab(poseStack, buffer, texture, CardBacks.textureFor(card),
                    CardMesh.TABLE_FRONT_Z, CardMesh.THICKNESS, false, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    // ==================== 对局中：战场 ====================

    private void renderBoard(DuelTableBlockEntity table, PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        float dirZ = pairDirZ(table);
        // 房主半场在 facing 反向外侧（主桌侧），客人半场在 facing 外侧（配桌侧）
        float hostZ = 0.5F + dirZ * 0.25F;
        float guestZ = 0.5F + dirZ * 0.75F;

        poseStack.pushPose();
        renderBoardHalf(table.getHostData(), hostZ, poseStack, buffer, packedLight, packedOverlay);
        renderBoardHalf(table.getGuestData(), guestZ, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderBoardHalf(DuelPlayerData data, float halfCenterZ, PoseStack poseStack,
                                 MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack[] board = data.getBoard();
        float step = BOARD_W + BOARD_GAP;
        float start = (BOARD_SLOTS - 1) * step / 2F;
        for (int i = 0; i < BOARD_SLOTS; i++) {
            ItemStack card = board[i];
            if (card.isEmpty()) {
                continue;
            }
            ResourceLocation texture = cardTexture(card);
            if (texture == null) {
                continue;
            }
            float cx = 0.5F + (i * step - start);
            poseStack.pushPose();
            poseStack.translate(cx - BOARD_W / 2F, CARD_Y, halfCenterZ + BOARD_H / 2F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
            poseStack.scale(BOARD_W / CardMesh.TEX_W, BOARD_H / CardMesh.TEX_H, 1F);
            CardMesh.renderSlab(poseStack, buffer, texture, CardBacks.textureFor(card),
                    CardMesh.TABLE_FRONT_Z, CardMesh.THICKNESS, false, packedLight, packedOverlay);
            // 实时数值层：召唤物显示"实际战力"（自身数值 + 该槽附着装备的加成 / 耐久）
            CardValueOverlay.drawInCardSpace(poseStack, buffer, card, data.getEquipped()[i],
                    CardValueOverlay.layoutFor(card), CardMesh.TABLE_FRONT_Z, packedLight);
            poseStack.popPose();
        }
    }

    // ==================== 入座/准备状态浮字 ====================

    /**
     * 对局前：在双方半场上空以原版名牌式 billboard 显示"谁已入座、是否已准备"。
     */
    private void renderStatusText(DuelTableBlockEntity table, PoseStack poseStack, MultiBufferSource buffer,
                                  int packedLight) {
        Level level = table.getLevel();
        if (level == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float dirZ = pairDirZ(table);
        float hostZ = 0.5F + dirZ * 0.25F;
        float guestZ = 0.5F + dirZ * 0.75F;
        renderStatusLine(mc, poseStack, buffer, table, level, table.getHostUuid(),
                table.getHostData().isDeckReady(), 0.5F + dirZ * 0.08F, hostZ, packedLight);
        renderStatusLine(mc, poseStack, buffer, table, level, table.getGuestUuid(),
                table.getGuestData().isDeckReady(), 0.5F - dirZ * 0.08F, guestZ, packedLight);
    }

    private void renderStatusLine(Minecraft mc, PoseStack poseStack, MultiBufferSource buffer,
                                  DuelTableBlockEntity table, Level level, @Nullable UUID uuid,
                                  boolean ready, float localX, float localZ, int packedLight) {
        if (uuid == null) {
            return;
        }
        Player player = level.getPlayerByUUID(uuid);
        String name = player != null ? player.getGameProfile().getName() : "?";
        Component line = Component.translatable(
                ready ? "cardduel.hud.status_ready" : "cardduel.hud.status_waiting", name);
        float textWidth = mc.font.width(line);

        poseStack.pushPose();
        poseStack.translate(table.getBlockPos().getX() + localX,
                table.getBlockPos().getY() + 1.45F,
                table.getBlockPos().getZ() + localZ);
        // 名牌式 billboard：始终面向相机
        poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());
        poseStack.scale(0.025F, -0.025F, 0.025F);
        mc.font.drawInBatch(line, -textWidth / 2F, 0F, ready ? 0xFF81C784 : 0xFFBDBDBD, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    // ==================== 工具 ====================

    /**
     * 牌堆命中检测：世界 X/Z → 哪一方的牌堆（0=房主，1=客人），未命中返回 -1。
     * 与 renderPile 共用同一套位置，保证悬停命中与实际渲染一致。
     */
    public static int pileHalfAt(DuelTableBlockEntity table, double worldX, double worldZ) {
        float dirZ = pairDirZ(table);
        double localX = worldX - table.getBlockPos().getX();
        double localZ = worldZ - table.getBlockPos().getZ();
        double hostZ = 0.5D + dirZ * 0.25D;
        double guestZ = 0.5D + dirZ * 0.75D;
        double half = Math.max(DECK_W, DECK_H) / 2D + 0.02D;
        if (Math.abs(localX - 0.5D) <= half && Math.abs(localZ - hostZ) <= half) {
            return 0;
        }
        if (Math.abs(localX - 0.5D) <= half && Math.abs(localZ - guestZ) <= half) {
            return 1;
        }
        return -1;
    }

    private static ResourceLocation cardTexture(ItemStack card) {
        if (!(card.getItem() instanceof CardDataAccessor accessor)) {
            return null;
        }
        Optional<ClientCardIndex> index = CdAPI.getClientCardIndex(accessor.getCardId(card));
        return index.map(ClientCardIndex::getTexture).orElse(null);
    }
}
