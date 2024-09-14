package net.mcczai.cardduel.client.event;


import net.mcczai.cardduel.client.resource.ClientReloadManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(value = Dist.CLIENT,bus = EventBusSubscriber.Bus.MOD)
public class ReloadResourceEvent {
    public static final ResourceLocation BLOCK_ATLAS_TEXTURE = ResourceLocation.tryBySeparator("textures/default/blocks.png",':');

    @SubscribeEvent
    public static void onTextureStitchEventPost(@NotNull TextureAtlasStitchedEvent event) {
        if (BLOCK_ATLAS_TEXTURE.equals(event.getAtlas().location())){
            ClientReloadManager.reloadAllPack();
        }
    }

}
