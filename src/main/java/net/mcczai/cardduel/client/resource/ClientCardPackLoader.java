package net.mcczai.cardduel.client.resource;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mcczai.cardduel.API.resource.ResourceManager;
import net.mcczai.cardduel.client.resource.loader.ClientCardIndexLoader;
import net.mcczai.cardduel.client.resource.loader.asset.LanguageLoader;
import net.mcczai.cardduel.client.resource.loader.asset.PackInfoLoader;
import net.mcczai.cardduel.client.resource.loader.asset.TextureLoader;
import net.mcczai.cardduel.client.resource.serialize.ItemStackSerializer;
import net.mcczai.cardduel.config.common.OtherConfig;
import net.mcczai.cardduel.util.GetJarResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
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

@OnlyIn(Dist.CLIENT)
public class ClientCardPackLoader {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ItemStack.class,new ItemStackSerializer())
            .create();

    public static final Map<ResourceLocation, ClientCardIndex> CARD_INDEX = Maps.newHashMap();

    public static void init(){
        createFolder();
        checkDefaultPack();
    }
/*
读取资源文件
 */
    public static void reloadAsset(){
        ClientAssetManager.INSTANCE.clearAll();

        File[] files = FOLDER.toFile().listFiles(((dir, name) -> true));
        if (files != null){
            readAsset(files);
        }
    }
/*
读取定义文件
 */
    public static void reloadIndex(){
        CARD_INDEX.clear();

        ClientCardIndexLoader.loadCardIndex();
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
                if (PackInfoLoader.load(zipFile,path)){
                    continue;
                }
                if (TextureLoader.load(zipFile,path)){
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

    private static void readDirAsset(File root){
        if (root.isDirectory()){
            LanguageLoader.load(root);
            PackInfoLoader.load(root);
            TextureLoader.load(root);
        }
    }

    private static void checkDefaultPack() {
        if (!OtherConfig.DEFAULT_PACK_DEBUG.get()){
            for (ResourceManager.ExtraEntry entry : ResourceManager.EXTRA_ENTRIES){
                GetJarResources.copyModDirectory(entry.modMainClass(),entry.srcPath(),FOLDER, entry.extraDirName());
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
