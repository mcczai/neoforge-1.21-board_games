package net.mcczai.cardduel.event;

import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.resources.DedicatedServerReloadManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(value = Dist.DEDICATED_SERVER,modid = CardduelMod.MODID,bus = EventBusSubscriber.Bus.MOD)
public class CommonLoadPack {
    @SubscribeEvent
    public static void loadCardPack(FMLCommonSetupEvent commonSetupEvent){
        DedicatedServerReloadManager.loadCardPack();
    }
}
