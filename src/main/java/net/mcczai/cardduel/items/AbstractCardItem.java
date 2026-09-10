package net.mcczai.cardduel.items;

import net.mcczai.cardduel.API.CdAPI;
import net.mcczai.cardduel.API.item.CardRarity;
import net.mcczai.cardduel.API.item.CardTabType;
import net.mcczai.cardduel.client.CardTooltip;
import net.mcczai.cardduel.items.builder.CardItemBuilder;
import net.mcczai.cardduel.resources.CommonCardIndex;
import net.mcczai.cardduel.resources.pojo.CardDataPOJO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractCardItem extends Item implements ICard {
    public AbstractCardItem(Properties properties) {
        super(properties);
    }

    /**
     * 右键时触发的事件
     */
    public abstract void rClick(ItemStack cardItem, LivingEntity Player, BlockPos blockPos);


    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        ResourceLocation cardId = this.getCardId(stack);
        Optional<CommonCardIndex> cardIndex = CdAPI.getCommonCardIndex(cardId);
        if (cardIndex.isPresent()){
            String name = cardIndex.get().getPojo().getName();
            if (StringUtils.isBlank(name)){
                name = "custom.cardduel.err.no_name";
            }
            MutableComponent component = Component.translatable(name);
            // 稀有度着色：新建的卡堆栈带 RARITY 组件、由原版着色；
            // 老存档里已存在的卡堆栈没有该组件，这里用同一套原版稀有度补上样式，保证观感一致。
            if (!stack.has(DataComponents.RARITY)) {
                component.withStyle(CardRarity.byName(cardIndex.get().getPojo().getRarity()).getStyleModifier());
            }
            return component;
        }
        return super.getName(stack);
    }

    /**
     * 卡牌 tooltip：挂上自定义组件（卡牌描述 + 攻击/生命/费用数值行）。
     * 仅客户端渲染 tooltip 时被调用；组件本身不含客户端专属类型，可安全放在公共侧。
     */
    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        return CardTooltip.of(stack).map(tooltip -> (TooltipComponent) tooltip);
    }

    /**
     *根据卡牌类型进行分类注册(是否需要？)
     */
    public static @NotNull NonNullList<ItemStack> fillItemTab(CardTabType type){
        NonNullList<ItemStack> stacks = NonNullList.create();
        String key = type.name().toLowerCase(Locale.US);
        CdAPI.getAllCommonCardIndex().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->{
            CommonCardIndex index = entry.getValue();
            CardDataPOJO cardDataPOJO = index.getCardData();
            if (!key.equals(cardDataPOJO.getTYPE())) {
                return;
            }
            ItemStack itemStack = CardItemBuilder.create()
                    .setId(entry.getKey())
                    .setHP(cardDataPOJO.getHP())
                    .setMP(cardDataPOJO.getMP())
                    .setATK(cardDataPOJO.getATK())
                    .setSkill(cardDataPOJO.getSKILL())
                    .setType(cardDataPOJO.getTYPE())
                    .setTribe(cardDataPOJO.getTRIBE())
                    .build();
            stacks.add(itemStack);
        });
        return stacks;
    }
}
