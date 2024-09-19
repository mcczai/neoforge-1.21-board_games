package net.mcczai.cardduel.resources;

public class DedicatedServerReloadManager {

    public static void loadCardPack(){
        CommonCardPackLoader.init();
        CommonCardPackLoader.reloadAsset();
        CommonCardPackLoader.reloadIndex();
    }

}
