package net.mcczai.cardduel.client.resource.loader.asset;


import net.mcczai.cardduel.API.CdAPI;
import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.client.resource.ClientCardIndex;
import net.mcczai.cardduel.resources.CardIndexPOJO;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;


import static net.mcczai.cardduel.client.resource.ClientCardPackLoader.CARD_INDEX;


public final class ClientCardIndexLoader {
    private static final Marker MARKER = MarkerManager.getMarker("ClientCardIndexLoader");

    public static void loadAmmoIndex() {
        CdAPI.getAllClientAmmoIndex().forEach(index -> {
            ResourceLocation id = index.getKey();
            CardIndexPOJO pojo = index.getValue().getPojo();
            try {
                CARD_INDEX.put(id, ClientCardIndex.getInstance(pojo));
            } catch (IllegalArgumentException exception) {
                CardduelMod.LOGGER.warn(MARKER, "{} index file read fail!", id);
                exception.printStackTrace();
            }
        });
    }
}
