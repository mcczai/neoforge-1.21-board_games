package net.mcczai.cardduel.init;

import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.items.inventory.CardBundleMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuType {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CardduelMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CardBundleMenu>> CARD_BUNDLE_MENU =
            MENUS.register("card_bundle_menu", ()-> new MenuType<>(CardBundleMenu::new, FeatureFlags.VANILLA_SET));

}
