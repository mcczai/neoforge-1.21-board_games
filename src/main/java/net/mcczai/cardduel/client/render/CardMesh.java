package net.mcczai.cardduel.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * 卡牌"薄盒"网格：正面（卡面贴图）+ 背面（卡背贴图）+ 四条侧边，
 * 让卡牌像普通物品一样有一格像素级别的厚度（默认 1/64 单位）。
 *
 * <p>调用约定：调用方必须先把 pose 调整到 <b>卡面像素空间</b>——
 * 原点 = 卡面左上角，x∈[0,48]，y∈[0,64]，+y 指向卡面下方，1 单位 = 1 卡面像素。
 * z 方向沿用当前 pose 的单位（不被 x/y 缩放影响），+z 朝向观察者：
 * 正面在 {@code frontZ}，背面在 {@code frontZ - thickness}。
 *
 * <p>统一使用 NoCull 渲染类型，因此顶点绕序不影响可见性（法线对实体光照也不起作用），
 * 侧边直接取卡背贴图的边缘 1px 拉伸作为"卡纸切面"的颜色。
 */
@OnlyIn(Dist.CLIENT)
public final class CardMesh {

    /** 贴图原始尺寸（所有卡面/卡背统一 48×64） */
    public static final float TEX_W = 48F;
    public static final float TEX_H = 64F;

    /** 卡牌厚度：1/64 单位（卡纸手感） */
    public static final float THICKNESS = 1F / 64F;

    /** 物品形态（手持/展示框/掉落物）正面 z：与原版平面物品的前表面一致（1/32），保证展示框里贴合 */
    public static final float ITEM_FRONT_Z = 1F / 32F;

    /** 桌面形态（战场/牌组预览）正面 z：卡片平放，正面（朝上）在 1/64 */
    public static final float TABLE_FRONT_Z = 1F / 64F;

    private CardMesh() {
    }

    /**
     * 绘制一张有厚度的卡牌。
     *
     * @param frontTexture 卡面贴图
     * @param backTexture  卡背贴图（背面与侧边共用）
     * @param frontZ       正面所在的 z
     * @param thickness    厚度（一般为 {@link #THICKNESS}）
     * @param hasFoil      是否附魔光效（卡牌目前恒为 false）
     */
    public static void renderSlab(PoseStack poseStack, MultiBufferSource buffer,
                                  ResourceLocation frontTexture, ResourceLocation backTexture,
                                  float frontZ, float thickness, boolean hasFoil,
                                  int packedLight, int packedOverlay) {
        float backZ = frontZ - thickness;
        Matrix4f pose = poseStack.last().pose();

        // 正面：卡面贴图（uv(0,0) = 卡面左上角）
        VertexConsumer front = ItemRenderer.getFoilBufferDirect(buffer,
                RenderType.entityCutoutNoCull(frontTexture), false, hasFoil);
        addQuad(pose, front,
                0F, 0F, frontZ, 0F, 0F,
                TEX_W, 0F, frontZ, 1F, 0F,
                TEX_W, TEX_H, frontZ, 1F, 1F,
                0F, TEX_H, frontZ, 0F, 1F,
                packedLight, packedOverlay);

        // 背面：卡背贴图，u 镜像（从背后看时图案正立）
        VertexConsumer back = ItemRenderer.getFoilBufferDirect(buffer,
                RenderType.entityCutoutNoCull(backTexture), false, hasFoil);
        addQuad(pose, back,
                0F, 0F, backZ, 1F, 0F,
                TEX_W, 0F, backZ, 0F, 0F,
                TEX_W, TEX_H, backZ, 0F, 1F,
                0F, TEX_H, backZ, 1F, 1F,
                packedLight, packedOverlay);

        // 四条侧边：取卡背贴图的边缘 1px 作为卡纸切面
        float uEdge = 1F / TEX_W;
        float vEdge = 1F / TEX_H;
        float uMax = 1F - uEdge;
        float vMax = 1F - vEdge;

        // 左边 x=0
        addQuad(pose, back,
                0F, 0F, frontZ, 0F, 0F,
                0F, TEX_H, frontZ, uEdge, 1F,
                0F, TEX_H, backZ, uEdge, 1F,
                0F, 0F, backZ, 0F, 0F,
                packedLight, packedOverlay);

        // 右边 x=TEX_W
        addQuad(pose, back,
                TEX_W, 0F, frontZ, 1F, 0F,
                TEX_W, 0F, backZ, uMax, 0F,
                TEX_W, TEX_H, backZ, uMax, 1F,
                TEX_W, TEX_H, frontZ, 1F, 1F,
                packedLight, packedOverlay);

        // 上边 y=0
        addQuad(pose, back,
                0F, 0F, frontZ, 0F, 0F,
                TEX_W, 0F, frontZ, 1F, 0F,
                TEX_W, 0F, backZ, 1F, vEdge,
                0F, 0F, backZ, 0F, vEdge,
                packedLight, packedOverlay);

        // 下边 y=TEX_H
        addQuad(pose, back,
                0F, TEX_H, frontZ, 0F, 1F,
                TEX_W, TEX_H, frontZ, 1F, 1F,
                TEX_W, TEX_H, backZ, 1F, vMax,
                0F, TEX_H, backZ, 0F, vMax,
                packedLight, packedOverlay);
    }

    /**
     * 顶点写入（法线统一 (0,0,1)：卡牌这类平面实体光照不参与方向着色，仅作占位）。
     */
    private static void addQuad(Matrix4f pose, VertexConsumer consumer,
                                float x0, float y0, float z0, float u0, float v0,
                                float x1, float y1, float z1, float u1, float v1,
                                float x2, float y2, float z2, float u2, float v2,
                                float x3, float y3, float z3, float u3, float v3,
                                int packedLight, int packedOverlay) {
        consumer.addVertex(pose, x0, y0, z0).setColor(255, 255, 255, 255).setUv(u0, v0)
                .setOverlay(packedOverlay).setLight(packedLight).setNormal(0F, 0F, 1F);
        consumer.addVertex(pose, x1, y1, z1).setColor(255, 255, 255, 255).setUv(u1, v1)
                .setOverlay(packedOverlay).setLight(packedLight).setNormal(0F, 0F, 1F);
        consumer.addVertex(pose, x2, y2, z2).setColor(255, 255, 255, 255).setUv(u2, v2)
                .setOverlay(packedOverlay).setLight(packedLight).setNormal(0F, 0F, 1F);
        consumer.addVertex(pose, x3, y3, z3).setColor(255, 255, 255, 255).setUv(u3, v3)
                .setOverlay(packedOverlay).setLight(packedLight).setNormal(0F, 0F, 1F);
    }
}
