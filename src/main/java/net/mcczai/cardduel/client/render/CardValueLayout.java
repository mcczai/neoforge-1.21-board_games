package net.mcczai.cardduel.client.render;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * 卡面数值槽布局预设。
 * 坐标一律为 <b>卡面像素</b>（48×64，左上角原点），与贴图 1:1 对应，
 * 因此换卡面美术时只需改这里的锚点。
 *
 * <ul>
 *   <li>{@link #SUMMON_3SLOT}：召唤卡 —— 攻击 / 生命 / 费用三槽（锚点取自现有卡面图标中心）</li>
 *   <li>{@link #COST_ONLY}：非召唤卡 —— 仅费用，位于卡面下方中央</li>
 *   <li>{@link #NONE}：不渲染（硬币卡、无卡包索引的卡）</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public enum CardValueLayout {

    /** 召唤卡：铁剑 (13,47) / 红心 (24,46) / 绿宝石 (36,46) */
    SUMMON_3SLOT(List.of(
            new Slot(Attr.ATK, 13, 47),
            new Slot(Attr.HP, 24, 46),
            new Slot(Attr.MP, 36, 46)
    )),

    /** 非召唤卡：仅费用，底部中央（对齐现有卡面底部宝石 x24..26 / y58..60） */
    COST_ONLY(List.of(
            new Slot(Attr.MP, 24, 58)
    )),

    NONE(List.of());

    /** 贴图原始尺寸（所有卡面贴图统一 48×64） */
    public static final float TEX_W = 48F;
    public static final float TEX_H = 64F;

    public enum Attr {
        ATK, HP, MP
    }

    /** 槽位：属性 + 卡面像素锚点（数字以锚点为中心绘制） */
    public record Slot(Attr attr, int x, int y) {
    }

    private final List<Slot> slots;

    CardValueLayout(List<Slot> slots) {
        this.slots = slots;
    }

    public List<Slot> slots() {
        return slots;
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    /**
     * 按卡包索引里的 type 选择布局：summon → 三槽，其余 → 仅费用。
     */
    public static CardValueLayout of(String cardType) {
        return "summon".equals(cardType) ? SUMMON_3SLOT : COST_ONLY;
    }
}
