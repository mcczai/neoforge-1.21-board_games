package net.mcczai.cardduel.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mcczai.cardduel.API.CdAPI;
import net.mcczai.cardduel.API.item.nbt.CardDataAccessor;
import net.mcczai.cardduel.client.render.CardBacks;
import net.mcczai.cardduel.client.render.CardMesh;
import net.mcczai.cardduel.client.render.CardValueOverlay;
import net.mcczai.cardduel.client.resource.ClientCardIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 卡牌物品渲染（背包 / 快捷栏 / 手持 / 展示框 / 掉落物）。
 *
 * <p>卡面为 48×64 的 3:4 竖版贴图，在物品框的 0..1 空间里取"高占满、宽 0.75、水平居中"，
 * 并通过 {@link CardMesh} 画成有厚度（1/64）的薄盒：正面卡面 + 背面卡背 + 侧边。
 *
 * <p>数值层仅在非 GUI 上下文绘制（GUI 格子只有 12×16px，数字不可读，改由 tooltip 呈现）。
 */
public class CardItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static CardItemRenderer INSTANCE;

    public static CardItemRenderer getInstance() {
        if (INSTANCE == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                throw new IllegalStateException("CardItemRenderer accessed before Minecraft is initialized");
            }
            INSTANCE = new CardItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return INSTANCE;
    }

    private CardItemRenderer(BlockEntityRenderDispatcher dispatcher, net.minecraft.client.model.geom.EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof CardDataAccessor cardAccessor)) {
            return;
        }

        ResourceLocation cardId = cardAccessor.getCardId(stack);
        Optional<ClientCardIndex> cardIndex = CdAPI.getClientCardIndex(cardId);

        ResourceLocation texture = cardIndex.map(ClientCardIndex::getTexture).orElse(null);
        if (texture == null) {
            return;
        }

        // 原版管线（ItemRenderer.java:124 的 translate(-0.5,-0.5,-0.5)）已把局部 0..1 定位到槽位：
        // 1.0 单位 = 16px = 一个槽位，(0,0) 为槽位左下角、+Y 朝上。
        final float W = 12F / 16F;          // 48:64 = 3:4，高占满槽位、宽 0.75
        final float H = 1F;
        final float X0 = (1F - W) / 2F;     // 0.125，水平居中

        poseStack.pushPose();
        // 进入"卡面像素空间"：原点 = 卡面左上角，+y 沿卡面向下（y 缩放取负以适配 +Y 朝上的物品空间）
        poseStack.translate(X0, H, 0F);
        poseStack.scale(W / CardMesh.TEX_W, -H / CardMesh.TEX_H, 1F);

        CardMesh.renderSlab(poseStack, buffer, texture, CardBacks.textureFor(stack),
                CardMesh.ITEM_FRONT_Z, CardMesh.THICKNESS, stack.hasFoil(), packedLight, packedOverlay);

        // 实时数值层：手持 / 展示框 / 掉落物照常绘制；GUI（背包/创造栏/快捷栏）跳过
        if (displayContext != ItemDisplayContext.GUI) {
            CardValueOverlay.drawInCardSpace(poseStack, buffer, stack, null,
                    CardValueOverlay.layoutFor(stack), CardMesh.ITEM_FRONT_Z, packedLight);
        }

        poseStack.popPose();
    }
}
