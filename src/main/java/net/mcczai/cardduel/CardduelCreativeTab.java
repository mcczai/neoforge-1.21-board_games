package net.mcczai.cardduel;

import net.mcczai.cardduel.init.ModItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.mcczai.cardduel.CardduelMod.MODID;

public class CardduelCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CARDDUEL_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CARDDUEL_TAB = CARDDUEL_TABS.register("cardduel_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cardduel"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItem.CARD_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItem.CARD_ITEM.get());
            }).build());
}
