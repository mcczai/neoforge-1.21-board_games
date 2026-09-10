package net.mcczai.cardduel.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mcczai.cardduel.API.CdAPI;
import net.mcczai.cardduel.client.resource.ClientCardIndex;
import net.mcczai.cardduel.items.CardTooltipPart;
import net.mcczai.cardduel.items.ICard;
import net.mcczai.cardduel.resources.pojo.CardDataPOJO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 卡面数值覆盖层：把实时数值叠在卡面贴图之上（贴图本身不含数字）。
 *
 * <p>数值 = 卡牌自身组件值 + 装备加成（装备加成只在结算时临时相加，见 DuelEngine，
 * 所以这里必须自行相加才能显示"实际战力"）。
 *
 * <p>颜色：
 * <ul>
 *   <li>卡面数字：白=与基准相同，绿=强化，红=削弱（费用反向：更便宜=强化）</li>
 *   <li>tooltip 数字：按属性固定色 —— 攻击红 / 生命绿 / 费用金</li>
 * </ul>
 *
 * <p>坐标系：全部按"卡面像素"（48×64）定位，绘制时缩放到当前 pose，
 * 所以同一套锚点在桌面、手牌 HUD、手持/展示框里都成立。
 */
@OnlyIn(Dist.CLIENT)
public final class CardValueOverlay {

    /** 卡面：与基准相同 */
    public static final int COLOR_FACE_NORMAL = 0xFFFFFF;
    /** 卡面：强化 */
    public static final int COLOR_FACE_BUFF = 0x55FF55;
    /** 卡面：削弱 */
    public static final int COLOR_FACE_NERF = 0xFF5555;

    /** tooltip：攻击（红） */
    public static final int COLOR_TOOLTIP_ATK = 0xFF5555;
    /** tooltip：生命（绿） */
    public static final int COLOR_TOOLTIP_HP = 0x55FF55;
    /** tooltip：费用（金） */
    public static final int COLOR_TOOLTIP_MP = 0xFFD700;
    /** tooltip：分隔符 */
    public static final int COLOR_TOOLTIP_SEPARATOR = 0xAAAAAA;

    /** 数字相对锚点的垂直偏移（卡面像素）：让 8px 高的字形在锚点上下居中 */
    private static final float TEXT_Y_OFFSET = 4F;

    /** 数字相对卡面的物理抬升（避免 z-fighting 的最小量，配合 POLYGON_OFFSET 几乎不可见） */
    private static final float Z_EPSILON = 0.0002F;

    private CardValueOverlay() {
    }

    /**
     * 一屏数值视图。state：1=强化，0=与基准相同，-1=削弱。
     */
    public record Values(int atk, int hp, int mp, int atkState, int hpState, int mpState) {

        public int value(CardValueLayout.Attr attr) {
            return switch (attr) {
                case ATK -> atk;
                case HP -> hp;
                case MP -> mp;
            };
        }

        public int state(CardValueLayout.Attr attr) {
            return switch (attr) {
                case ATK -> atkState;
                case HP -> hpState;
                case MP -> mpState;
            };
        }

        /** 卡面颜色：白 / 绿（强化）/ 红（削弱） */
        public int faceColor(CardValueLayout.Attr attr) {
            int s = state(attr);
            if (s > 0) {
                return COLOR_FACE_BUFF;
            }
            return s < 0 ? COLOR_FACE_NERF : COLOR_FACE_NORMAL;
        }
    }

    /** tooltip 一行里的一段（文本 + 颜色） */
    public record Segment(String text, int color) {
    }

    // ==================== 布局选择 ====================

    /**
     * 依据卡包索引选择布局；无索引（硬币卡 / 非法卡）→ 不渲染数值。
     */
    public static CardValueLayout layoutFor(ItemStack card) {
        Optional<ClientCardIndex> index = clientIndex(card);
        return index.map(i -> CardValueLayout.of(i.getType())).orElse(CardValueLayout.NONE);
    }

    private static Optional<ClientCardIndex> clientIndex(ItemStack card) {
        ICard access = ICard.getICardOrNull(card);
        if (access == null) {
            return Optional.empty();
        }
        return CdAPI.getClientCardIndex(access.getCardId(card));
    }

    // ==================== 数值解析 ====================

    /**
     * 解析卡面显示值。
     *
     * @param card 卡牌
     * @param equip 该卡所在战场槽附着的装备（手牌/背包传 null）；装备 ATK 计入攻击、装备 HP（耐久）计入生命
     */
    public static Values resolve(ItemStack card, @Nullable ItemStack equip) {
        ICard access = ICard.getICardOrNull(card);
        if (access == null) {
            return new Values(0, 0, 0, 0, 0, 0);
        }

        int atk = access.getATK(card);
        int hp = access.getHP(card);
        int mp = access.getMP(card);

        // 装备加成：生效逻辑见 DuelEngine（equipAtk / equipDurability）
        if (equip != null && !equip.isEmpty() && !equip.equals(card)) {
            ICard equipAccess = ICard.getICardOrNull(equip);
            if (equipAccess != null) {
                atk += equipAccess.getATK(equip);
                hp += equipAccess.getHP(equip);
            }
        }

        int baseAtk = atk;
        int baseHp = hp;
        int baseMp = mp;
        Optional<ClientCardIndex> index = clientIndex(card);
        if (index.isPresent()) {
            CardDataPOJO data = index.get().getCardData();
            if (data != null) {
                baseAtk = data.getATK();
                baseHp = data.getHP();
                baseMp = data.getMP();
            }
        }

        return new Values(
                atk, hp, mp,
                Integer.compare(atk, baseAtk),
                Integer.compare(hp, baseHp),
                // 费用：更低 = 强化，故与基准反向比较
                Integer.compare(baseMp, mp)
        );
    }

    // ==================== tooltip 数值行 ====================

    /**
     * 生成 tooltip 的数值行（一段一段，便于分段着色）：
     * 形如 {@code 3（攻击力）/5（生命值）/2（费用）}，召唤卡三组、其余卡只有费用。
     */
    public static List<Segment> tooltipSegments(ItemStack card, CardValueLayout layout) {
        List<Segment> segments = new ArrayList<>();
        if (layout.isEmpty()) {
            return segments;
        }
        Values values = resolve(card, null);
        for (CardValueLayout.Slot slot : layout.slots()) {
            if (!tooltipVisible(card, slot.attr())) {
                continue;
            }
            if (!segments.isEmpty()) {
                segments.add(new Segment(Component.translatable("cardduel.value.separator").getString(),
                        COLOR_TOOLTIP_SEPARATOR));
            }
            // 图标 + 数值，例如 ⚔3 ❤5 ◆2
            String icon = Component.translatable(iconKey(slot.attr())).getString();
            String text = Component.translatable("cardduel.value.entry",
                    icon, Integer.toString(values.value(slot.attr()))).getString();
            segments.add(new Segment(text, tooltipColor(slot.attr())));
        }
        return segments;
    }

    /** tooltip 固定属性色：攻击红 / 生命绿 / 费用金 */
    public static int tooltipColor(CardValueLayout.Attr attr) {
        return switch (attr) {
            case ATK -> COLOR_TOOLTIP_ATK;
            case HP -> COLOR_TOOLTIP_HP;
            case MP -> COLOR_TOOLTIP_MP;
        };
    }

    private static String iconKey(CardValueLayout.Attr attr) {
        return switch (attr) {
            case ATK -> "cardduel.value.icon.atk";
            case HP -> "cardduel.value.icon.hp";
            case MP -> "cardduel.value.icon.mp";
        };
    }

    /** 单项是否未被 HideFlags 隐藏 */
    private static boolean tooltipVisible(ItemStack card, CardValueLayout.Attr attr) {
        CardTooltipPart part = switch (attr) {
            case ATK -> CardTooltipPart.ATK_INFO;
            case HP -> CardTooltipPart.HP_INFO;
            case MP -> CardTooltipPart.MP_INFO;
        };
        return (CardTooltipPart.getHideFlags(card) & part.getMask()) == 0;
    }

    // ==================== 绘制入口 ====================

    /**
     * 牌桌世界渲染（战场卡）：pose 原点 = 卡面左上角，art +y 指向卡面下方（与贴图 v 同向）。
     */
    /**
     * 在<b>卡面像素空间</b>（原点=卡面左上角，x∈[0,48]，y∈[0,64]，+y 沿卡面向下）绘制数值。
     * 三个渲染现场都先把 pose 变换到该空间后再调用（见 CardMesh），
     * 因此这里只按锚点居中绘制，并把数字贴在卡牌正面上方一丁点。
     *
     * @param frontZ 卡牌正面的 z（{@link CardMesh#ITEM_FRONT_Z} 或 {@link CardMesh#TABLE_FRONT_Z}）
     */
    public static void drawInCardSpace(PoseStack poseStack, MultiBufferSource buffer, ItemStack card,
                                       @Nullable ItemStack equip, CardValueLayout layout,
                                       float frontZ, int packedLight) {
        // POLYGON_OFFSET：既压住卡面又不产生可见的"悬浮"间隙（物理偏移仅 0.2mm）
        draw(poseStack, buffer, resolve(card, equip), layout,
                0F, 0F, frontZ + Z_EPSILON, 1F, 1F,
                Font.DisplayMode.POLYGON_OFFSET, packedLight);
    }

    /**
     * 决斗手牌 HUD（2D 屏幕空间）：卡面顶边在 (x, y)。
     */
    public static void drawOnHud(GuiGraphics guiGraphics, ItemStack card, CardValueLayout layout,
                                 int x, int y, int width, int height) {
        draw(guiGraphics.pose(), guiGraphics.bufferSource(), resolve(card, null), layout,
                x, y, 0F,
                (float) width / CardValueLayout.TEX_W, (float) height / CardValueLayout.TEX_H,
                Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT);
    }

    /**
     * 核心绘制：把 pose 平移到卡面左上角并按 1 卡面像素 = 1 字体像素缩放，然后按锚点居中绘制各数字。
     * 位数增加时向两侧自然溢出（不做裁剪或缩字）。
     */
    private static void draw(PoseStack poseStack, MultiBufferSource buffer, Values values, CardValueLayout layout,
                             float originX, float originY, float originZ,
                             float scaleX, float scaleY, Font.DisplayMode displayMode, int packedLight) {
        if (layout.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        if (font == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(originX, originY, originZ);
        poseStack.scale(scaleX, scaleY, 1F);
        Matrix4f matrix = poseStack.last().pose();

        for (CardValueLayout.Slot slot : layout.slots()) {
            String text = Integer.toString(values.value(slot.attr()));
            float textWidth = font.width(text);
            font.drawInBatch(text,
                    slot.x() - textWidth / 2F,
                    slot.y() - TEXT_Y_OFFSET,
                    values.faceColor(slot.attr()),
                    true,
                    matrix, buffer,
                    displayMode, 0, packedLight);
        }

        poseStack.popPose();
    }
}
