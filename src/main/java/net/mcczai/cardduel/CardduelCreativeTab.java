package net.mcczai.cardduel;

import net.mcczai.cardduel.API.item.CardTabType;
import net.mcczai.cardduel.init.ModItem;
import net.mcczai.cardduel.items.AbstractCardItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.mcczai.cardduel.CardduelMod.MODID;

public class CardduelCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CARDDUEL_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TARP_TAB = CARDDUEL_TABS.register("tarp_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cardduel.tarp"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItem.CARD_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.acceptAll(AbstractCardItem.ItemTab(CardTabType.TRAP));
            }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MANA_TAB = CARDDUEL_TABS.register("mana_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cardduel.mana"))
                    .withTabsBefore(TARP_TAB.getId())
                    .icon(() -> ModItem.CARD_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.acceptAll(AbstractCardItem.ItemTab(CardTabType.MANA));
            }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EQUIP_TAB = CARDDUEL_TABS.register("equip_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cardduel.equip"))
            .withTabsBefore(MANA_TAB.getId())
            .icon(() -> ModItem.CARD_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.acceptAll(AbstractCardItem.ItemTab(CardTabType.EQUIP));
            }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SUMMON_TAB = CARDDUEL_TABS.register("summon_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cardduel.summon"))
            .withTabsBefore(EQUIP_TAB.getId())
            .icon(() -> ModItem.CARD_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.acceptAll(AbstractCardItem.ItemTab(CardTabType.SUMMON));
            }).build());
}
