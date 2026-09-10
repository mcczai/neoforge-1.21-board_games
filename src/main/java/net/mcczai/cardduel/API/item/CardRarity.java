package net.mcczai.cardduel.API.item;

import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * 卡包 rarity 字段 → 原版 {@link Rarity} 的映射。
 *
 * <p>卡包用中文词描述稀有度（普通 / 罕见 / 稀有 / 传说），这里统一映射到 Minecraft 的稀有度等级，
 * 由原版系统负责着色（COMMON 白 / UNCOMMON 黄 / RARE 青 / EPIC 紫）。
 * 同时接受英文 id（common / uncommon / rare / epic）以便自定义卡包直接书写。
 */
public final class CardRarity {

    private CardRarity() {
    }

    /**
     * @param name 卡包里的 rarity 值（可为 null / 未知）
     * @return 对应的原版稀有度，未知一律回落 {@link Rarity#COMMON}
     */
    public static Rarity byName(@Nullable String name) {
        if (name == null) {
            return Rarity.COMMON;
        }
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "uncommon", "罕见" -> Rarity.UNCOMMON;
            case "rare", "稀有" -> Rarity.RARE;
            case "epic", "legendary", "传说" -> Rarity.EPIC;
            default -> Rarity.COMMON;
        };
    }
}
