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
     * @param livingEntity
     * @return 判断主手是否手持卡牌
     */
    static boolean mainHandHoldCard(LivingEntity livingEntity)
    {
        return livingEntity.getMainHandItem().getItem() instanceof ICard;
    }

    /**
     *读取手中卡牌的血量
     */
    int getHp(ItemStack Card);
    /**
     * 获得手中卡牌的消耗
     */
    int getMp(ItemStack Card);

    /**
     *读取手中卡牌的攻击力
     */
    int getAtk(ItemStack Card);

    /**
     *读取手中卡牌的类型
     */
    int getType(ItemStack Card);

    /**
     *读取手中卡牌的技能效果
     */
    String getSkill(ItemStack Card);

    /**
     * 获得卡牌ID
     */
    ResourceLocation getCardId(ItemStack card);

    /**
     * 设置卡牌ID
     */
    void setCardId(ItemStack card,ResourceLocation cardId);
}
