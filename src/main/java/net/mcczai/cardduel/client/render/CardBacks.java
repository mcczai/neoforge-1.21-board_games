package net.mcczai.cardduel.client.render;

import net.mcczai.cardduel.CardduelMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 卡背贴图解析（卡背 = 卡牌有厚度后背面/侧边使用的贴图）。
 *
 * <p>现在只有一张内置占位卡背；结构上已为「在卡包袋里更换卡背」预留：
 * <ul>
 *   <li>{@link #register(ResourceLocation, ResourceLocation)} 供后续卡包/资源系统登记卡背款式</li>
 *   <li>{@link #setSelected(ResourceLocation)} 供后续写入玩家选择（卡包袋组件 / 客户端偏好）</li>
 *   <li>{@link #textureFor(ItemStack)} 保留卡牌参数，便于将来做「单卡指定卡背」</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class CardBacks {

    /** 内置占位卡背的 id */
    public static final ResourceLocation DEFAULT_ID = ResourceLocation.fromNamespaceAndPath(CardduelMod.MODID, "default");

    private static final Map<ResourceLocation, ResourceLocation> REGISTRY = new LinkedHashMap<>();

    static {
        REGISTRY.put(DEFAULT_ID,
                ResourceLocation.fromNamespaceAndPath(CardduelMod.MODID, "textures/item/card_back.png"));
    }

    /** 当前选用的卡背款式（后续由卡包袋/偏好驱动） */
    private static ResourceLocation selected = DEFAULT_ID;

    private CardBacks() {
    }

    /**
     * 登记一款卡背：id → 贴图路径。供后续卡包资源系统调用。
     */
    public static void register(ResourceLocation id, ResourceLocation texture) {
        REGISTRY.put(id, texture);
    }

    /**
     * 设定当前使用的卡背款式；id 未登记时回落到内置占位卡背。
     */
    public static void setSelected(@Nullable ResourceLocation id) {
        selected = id != null && REGISTRY.containsKey(id) ? id : DEFAULT_ID;
    }

    public static ResourceLocation getSelected() {
        return selected;
    }

    /**
     * 取某张卡应当使用的卡背贴图。
     *
     * @param card 卡牌（当前忽略，预留给"单卡指定卡背"）
     */
    public static ResourceLocation textureFor(@Nullable ItemStack card) {
        return REGISTRY.getOrDefault(selected, REGISTRY.get(DEFAULT_ID));
    }
}
