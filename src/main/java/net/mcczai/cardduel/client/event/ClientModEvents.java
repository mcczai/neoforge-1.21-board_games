package net.mcczai.cardduel.client.event;

import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.client.CardTooltip;
import net.mcczai.cardduel.client.ClientCardTooltip;
import net.mcczai.cardduel.client.duel.CardHoverTooltip;
import net.mcczai.cardduel.client.gui.screens.inventory.CardBagScreen;
import net.mcczai.cardduel.client.hud.BattleBoardHud;
import net.mcczai.cardduel.client.hud.DuelHandHud;
import net.mcczai.cardduel.client.renderer.blockentity.DuelTableBlockEntityRenderer;
import net.mcczai.cardduel.client.resource.ClientReloadManager;
import net.mcczai.cardduel.init.ModBlockEntities;
import net.mcczai.cardduel.init.ModMenuType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CardduelMod.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        BlockEntityRenderers.register(ModBlockEntities.DUELTABLE.get(), DuelTableBlockEntityRenderer::new);
        ClientReloadManager.reloadAllPack();
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuType.CARD_BAG_MENU.get(), CardBagScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CardTooltip.class, ClientCardTooltip::new);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // 对局 HUD 全部注册为最高层 GUI 层，保证绘制在所有原版/第三方层（含聊天栏）之上。
        // 注册顺序即绘制顺序：状态条 < 手牌 < 悬停 tooltip（tooltip 必须最顶，否则会被手牌盖住）。
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(CardduelMod.MODID, "duel_bars"),
                BattleBoardHud::renderLayer);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(CardduelMod.MODID, "duel_hand"),
                DuelHandHud::renderLayer);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(CardduelMod.MODID, "duel_tooltip"),
                CardHoverTooltip::renderLayer);
    }
}
