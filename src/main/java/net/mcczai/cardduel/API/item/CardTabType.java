package net.mcczai.cardduel.API.item;

import com.google.gson.annotations.SerializedName;

/**
 *  物品栏按卡牌类型分类：
 *   魔法卡、召唤卡、陷阱卡、装备卡
 */
public enum CardTabType {
//陷阱
    @SerializedName("trap")
    TRAP,
//魔法
    @SerializedName("mana")
    MANA,
//装备
    @SerializedName("equip")
    EQUIP,
//召唤
    @SerializedName("summon")
    SUMMON;
}
