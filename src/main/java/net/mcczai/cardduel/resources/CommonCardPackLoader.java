package net.mcczai.cardduel.resources;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mcczai.cardduel.API.resource.ResourceManager;
import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.config.common.OtherConfig;
import net.mcczai.cardduel.resources.loader.CardDataLoader;
import net.mcczai.cardduel.resources.loader.CommonCardIndexLoader;
import net.mcczai.cardduel.util.GetJarResources;
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

    public static final Map<ResourceLocation, CommonCardIndex> CARD_INDEX = Maps.newHashMap();

    @Deprecated//TODO :这里是创建目录的地方，未检查出客户端为何不执行前此TODO不要删除

    /**
     *创建目录，并复制默认包至该目录
     */
    public static void init(){
        createFolder();
        checkDefaultPack();
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
    public static void reloadIndex() {
        CARD_INDEX.clear();

        File[] files = FOLDER.toFile().listFiles((dir, name) -> true);
        if (files != null) {
            readIndex(files);
        }
    }


    private static void checkDefaultPack(){
        if (!OtherConfig.DEFAULT_PACK_DEBUG.get()){
            for (ResourceManager.ExtraEntry entry : ResourceManager.EXTRA_ENTRIES){
                GetJarResources.copyModDirectory(entry.modMainClass(), entry.srcPath(), FOLDER, entry.extraDirName());
            }
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

    private static void readIndex(File[] files) {
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".zip")) {
                readZipIndex(file);
            }
            if (file.isDirectory()) {
                File[] subFiles = file.listFiles((dir, name) -> true);
                if (subFiles == null) {
                    return;
                }
                for (File namespaceFile : subFiles) {
                    readDirIndex(namespaceFile);
                }
            }
        }
    }

    private static void readZipIndex(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> iteration = zipFile.entries();
            while (iteration.hasMoreElements()) {
                String path = iteration.nextElement().getName();
                CommonCardIndexLoader.loadCardIndex(path, zipFile);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void readDirIndex(File root) {
        if (root.isDirectory()) {
            try {
                CommonCardIndexLoader.loadCardIndex(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Optional<CommonCardIndex> getCardIndex(ResourceLocation registryName){
        return Optional.ofNullable(CARD_INDEX.get(registryName));
    }

    public static Set<Map.Entry<ResourceLocation, CommonCardIndex>> getAllCards(){
        return CARD_INDEX.entrySet();
    }
}
