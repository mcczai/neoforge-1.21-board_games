package net.mcczai.cardduel.API;

import net.mcczai.cardduel.CardduelMod;
import net.minecraft.resources.ResourceLocation;

/**
 * 这里存放默认资源位置，用于无法读取时的特殊加载
 */
public class DefaultAsset {
    //默认卡牌贴图
    public static final ResourceLocation DEFAULT_CARD_TEXTURE = ResourceLocation.fromNamespaceAndPath(CardduelMod.MODID,"default_card");

}
