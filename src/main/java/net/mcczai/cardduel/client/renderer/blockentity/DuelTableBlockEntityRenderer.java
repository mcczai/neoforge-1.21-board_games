package net.mcczai.cardduel.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mcczai.cardduel.API.CdAPI;
import net.mcczai.cardduel.API.item.nbt.CardDataAccessor;
import net.mcczai.cardduel.block.DuelTableBlock;
import net.mcczai.cardduel.block.entity.DuelTableBlockEntity;
import net.mcczai.cardduel.client.resource.ClientCardIndex;
import net.mcczai.cardduel.duel.DuelPhase;
import net.mcczai.cardduel.duel.DuelPlayerData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Optional;

/**
 * 牌桌渲染（坐标系说明：双桌沿南北向排列，渲染原点 = 本方块实体所在桌）。
 *  - 对局前（IDLE/SETUP/WAITING）：双方已提交牌组合并展示，9×3 铺满双桌桌面
 *  - 对局中（MULLIGAN/PLAYING）：双方战场 7+7 槽，各占半场，卡面朝上
 * 卡面 quad 经 rotateX(-90°) 后：本地 x → 世界 X；本地 y → 世界 -Z（北为负）。
 */
@OnlyIn(Dist.CLIENT)
public class DuelTableBlockEntityRenderer implements BlockEntityRenderer<DuelTableBlockEntity> {

    /** 卡面高度（桌面上方一点，避免 z-fighting） */
    private static final float CARD_Y = 1.01F;

    // 牌组展示（9×3，限 27 张）——卡面贴图为 48×64（3:4），长宽比必须一致否则预览会被挤扁
    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final float DECK_W = 2.4F / 16F;
    private static final float DECK_H = 3.2F / 16F;   // 2.4 : 3.2 = 3 : 4，与卡面贴图 48:64 对齐
    private static final float DECK_GAP_X = 0.25F / 16F;
    private static final float DECK_GAP_Z = 0.3F / 16F;

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
        List<ItemStack> deck = table.getDeck();
        if (deck.isEmpty()) {
            return;
        }
        float dirZ = pairDirZ(table);
        // 双桌中心（局部坐标）
        float centerX = 0.5F;
        float centerZ = 0.5F + dirZ * 0.5F;

        float stepX = DECK_W + DECK_GAP_X;
        float stepZ = DECK_H + DECK_GAP_Z;
        float startX = (COLUMNS - 1) * stepX / 2F;
        float startZ = (ROWS - 1) * stepZ / 2F;

        for (int i = 0; i < deck.size() && i < COLUMNS * ROWS; i++) {
            ItemStack card = deck.get(i);
            ResourceLocation texture = cardTexture(card);
            if (texture == null) {
                continue;
            }
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            // 卡中心：列沿 +X，行沿 -Z（本地 y → -Z）
            float cx = centerX + (col * stepX - startX);
            float cz = centerZ - (row * stepZ - startZ);
            poseStack.pushPose();
            poseStack.translate(cx - DECK_W / 2F, CARD_Y, cz + DECK_H / 2F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
            renderCardQuad(poseStack, buffer, texture, DECK_W, DECK_H, packedLight, packedOverlay);
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
            renderCardQuad(poseStack, buffer, texture, BOARD_W, BOARD_H, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    // ==================== 工具 ====================

    private static ResourceLocation cardTexture(ItemStack card) {
        if (!(card.getItem() instanceof CardDataAccessor accessor)) {
            return null;
        }
        Optional<ClientCardIndex> index = CdAPI.getClientCardIndex(accessor.getCardId(card));
        return index.map(ClientCardIndex::getTexture).orElse(null);
    }

    private static void renderCardQuad(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
                                       float width, float height, int packedLight, int packedOverlay) {
        Matrix4f pose = poseStack.last().pose();
        RenderType renderType = RenderType.entityCutout(texture);
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffer, renderType, false, false);
        consumer.addVertex(pose, 0, 0, 0).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        consumer.addVertex(pose, width, 0, 0).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        consumer.addVertex(pose, width, height, 0).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        consumer.addVertex(pose, 0, height, 0).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
    }
}
