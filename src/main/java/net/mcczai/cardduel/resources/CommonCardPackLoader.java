package net.mcczai.cardduel.resources;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.loader.CardDataLoader;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CommonCardPackLoader {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class,new ResourceLocation.Serializer())
            .create();
    /**
     * 这里定义文件目录
     */
    public static final Path FOLDER = Paths.get("config", CardduelMod.MODID,"custom");

    public static final Map<ResourceLocation, CardIndex> CARD_INDEX = Maps.newHashMap();

    /**
     *创建目录
     */
    public static void init(){
        createFolder();
    }

    public static void createFolder(){
        File folder = FOLDER.toFile();
        if(!folder.isDirectory()){
            try {
                Files.createDirectories(folder.toPath());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 读取卡包数据
     */
    public static void reloadAsset(){
        CardAssetManager.INSTANCE.clearAll();

        File[] files = FOLDER.toFile().listFiles(((dir, name) -> true));
        if (files != null){
            readAsset(files);
        }
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

    public static void readDirAsset(File root){
        if (root.isDirectory()){
            CardDataLoader.load(root);
        }
    }

    private static void readZipAsset(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> iteration = zipFile.entries();
            while (iteration.hasMoreElements()) {
                String path = iteration.nextElement().getName();
                CardDataLoader.load(zipFile, path);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static Optional<CardIndex> getCardIndex(ResourceLocation registryName){
        return Optional.ofNullable(CARD_INDEX.get(registryName));
    }

    public static Set<Map.Entry<ResourceLocation,CardIndex>> getAllGuns(){
        return CARD_INDEX.entrySet();
    }
}
