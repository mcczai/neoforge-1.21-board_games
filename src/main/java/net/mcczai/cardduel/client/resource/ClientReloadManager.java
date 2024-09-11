package net.mcczai.cardduel.client.resource;

import net.mcczai.cardduel.resources.CommonCardPackLoader;

public class ClientReloadManager {


    public static void reloadAllPack(){
        ClientCardPackLoader.init();

        CommonCardPackLoader.reloadAsset();
        ClientCardPackLoader.reloadAsset();

        CommonCardPackLoader.reloadIndex();
        ClientCardPackLoader.reloadIndex();
    }

    public static void cacheAll(){

    }
}
