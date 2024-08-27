package net.mcczai.cardduel.client.resource.loader;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mcczai.cardduel.client.resource.ClientAssetManager;
import net.mcczai.cardduel.client.resource.ClientCardIndex;
import net.mcczai.cardduel.client.resource.loader.asset.LanguageLoader;
import net.mcczai.cardduel.client.resource.loader.asset.PackInfoLoader;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.mcczai.cardduel.resources.CommonCardPackLoader.FOLDER;
import static net.mcczai.cardduel.resources.CommonCardPackLoader.readDirAsset;

@OnlyIn(Dist.CLIENT)
public class ClientCardPackLoader {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .create();

    public static final Map<ResourceLocation, ClientCardIndex> CARD_INDEX = Maps.newHashMap();

    public static void init(){
        createFolder();
    }

    private static void reloadAsset(){
        ClientAssetManager.INSTANCE.clearAll();

        File[] files = FOLDER.toFile().listFiles(((dir, name) -> true));
        if (files != null){
            readAsset(files);
        }
    }

    public static void reloadIndex(){

    }

    private static void readAsset(File[] files) {
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".zip")) {
                readZipAsset(file);
            }
            if (file.isDirectory()) {
                File[] subFiles = file.listFiles((dir, name) -> true);
                if (subFiles == null) {
                    return;
                }
                for (File namespaceFile : subFiles) {
                    readDirAsset(namespaceFile);
                }
            }
        }
    }

    private static void readZipAsset(File file) {
        try (ZipFile zipFile = new ZipFile(file)){
            Enumeration<? extends  ZipEntry> iteration = zipFile.entries();
            while (iteration.hasMoreElements()){
                String path = iteration.nextElement().getName();
                if (LanguageLoader.load(zipFile,path)){
                    continue;
                }
                PackInfoLoader.load(zipFile,path);
            }
        }catch (IOException ioException){
            ioException.printStackTrace();
        }
    }

    private static void createFolder() {
        File folder = FOLDER.toFile();
        if (!folder.isDirectory()) {
            try {
                Files.createDirectories(folder.toPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Set<Map.Entry<ResourceLocation,ClientCardIndex>> getAllCard(){
        return CARD_INDEX.entrySet();
    }

    public static Optional<ClientCardIndex> getCardIndex(ResourceLocation registryName){
        return Optional.ofNullable(CARD_INDEX.get(registryName));
    }
}
