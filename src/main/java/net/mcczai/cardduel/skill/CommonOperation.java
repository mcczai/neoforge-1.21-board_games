package net.mcczai.cardduel.skill;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class CommonOperation{
    public static final int CommonHp = 1;
    public static final int CommonAtk = 0;

    /**
     * 用来判定此次攻击是否造成召唤物死亡
     * @param ATK 攻击方攻击力
     * @param HP 被攻击方血量
     * @return 是否死亡
     */
    public static boolean deathVerdict(int ATK,int HP){
        return ATK >= HP;
    }

    /**
     * 普通死亡事件，先留空，预计是在牌桌内置一个卡牌堆作为墓场和弃牌堆，用过的牌/死亡的召唤物会进入这个牌堆(牌堆类似收纳袋)
     * @param itemStack 死亡的召唤物卡牌
     */
    public static void commonDeath(ItemStack itemStack){

    }


    /**
     *造成伤害事件
     * @param itemStack1 攻击召唤物
     * @param itemStack2 被攻击召唤物
     */
    public static void commonAttack(ItemStack itemStack1 ,ItemStack itemStack2){
        CompoundTag tag1 = itemStack1.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag tag2 = itemStack2.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag1.get("ATK") != null && tag2.get("HP") != null){
            int item1ATK = tag1.getInt("ATK");
            int item2HP = tag1.getInt("HP");
            //做一次是否死亡判断
            if (!deathVerdict(tag1.getInt("ATK"),tag2.getInt("HP"))){
                tag2.putInt("HP",item2HP - item1ATK);
                itemStack2.set(DataComponents.CUSTOM_DATA,CustomData.of(tag2));
            }else {
                tag2.putInt("HP",0);
                commonDeath(itemStack2);
            }
        }
    }

    /**
     * 增加召唤物血量事件(无上限？)
     * @param itemStack 目标召唤物
     * @param hp 增加的血量
     */
    public static void commonTreat(ItemStack itemStack ,int hp){
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.get("HP") != null){
            int itemHP = tag.getInt("HP");
            tag.putInt("HP",itemHP + hp);
            itemStack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        }
    }

    /**
     * 增加召唤物伤害事件
     * @param itemStack 目标召唤物
     * @param atk 增加的攻击力
     */
    public static void commonAtkUp(ItemStack itemStack , int atk){
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.get("ATK") != null){
            int itemHP = tag.getInt("HP");
            tag.putInt("ATK",itemHP + atk);
            itemStack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        }
    }

    /**
     * 减少召唤物伤害事件(最低为0)
     * @param itemStack 目标召唤物
     * @param atk 减少的攻击力
     */
    public static void commonAtkDown(ItemStack itemStack ,int atk){
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.get("ATK") != null){
            int itemHP = tag.getInt("HP");
            if ((itemHP - atk) < 1){
                tag.putInt("ATK",CommonAtk);
                itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }else {
                tag.putInt("ATK", itemHP - atk);
                itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

}
