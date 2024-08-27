package net.mcczai.cardduel.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface ICard {
    /**
     * @return 判断是否为ICard类型，否则返回null
     */
    static ICard getICardOrNull(@Nullable ItemStack stack) {
        if (stack == null) {
            return null;
        }
        if (stack.getItem() instanceof ICard iCard) {
            return iCard;
        }
        return null;
    }

    /**
     *
     * @return boolean 判断主手是否手持卡牌
     */
    static boolean mainHandHoldCard(LivingEntity livingEntity)
    {
        return livingEntity.getMainHandItem().getItem() instanceof ICard;
    }

    /**
     * 读取手中卡牌的血量
     */
    int getHP(ItemStack Card);

    void setHP(ItemStack Card,int amount);
    /**
     * 获得手中卡牌的消耗
     */
    int getMP(ItemStack Card);

    void setMP(ItemStack Card,int amount);

    /**
     *读取手中卡牌的攻击力
     */
    int getATK(ItemStack Card);

    void setATK(ItemStack Card,int amount);
    /**
     *读取手中卡牌的类型
     */
    int getType(ItemStack Card);

    void setType(ItemStack Card, int type);

    /**
     *读取手中卡牌的技能效果
     */
    String getSkill(ItemStack Card);

    void setSkill(ItemStack Card, String skill);

    /**
     * 获得卡牌ID
     */
    ResourceLocation getCardId(ItemStack card);

    void setCardId(ItemStack card,@Nullable ResourceLocation cardId);
}
