package net.mcczai.cardduel.resources.loader;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.resources.CardAssetManager;
import net.mcczai.cardduel.resources.CommonCardPackLoader;
import net.mcczai.cardduel.resources.pojo.CardDataPOJO;
import net.mcczai.cardduel.util.PathVisitor;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
    //网络包记得写
public final class CardDataLoader {
    private static final Marker MARKER = MarkerManager.getMarker("CardDataLoader");
    public static final Pattern CARD_DATA_PATTERN = Pattern.compile("^(\\w+)/cards/data/([\\w/]+)\\.json$");

    /**
     *ZIP格式加载
     */
    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher matcher = CARD_DATA_PATTERN.matcher(zipPath);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            String path = matcher.group(2);
            ZipEntry entry = zipFile.getEntry(zipPath);
            if (entry == null) {
                CardduelMod.LOGGER.warn(MARKER, "{} file don't exist", zipPath);
                return false;
            }
            try (InputStream stream = zipFile.getInputStream(entry)) {
                ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(namespace, path);
                String json = IOUtils.toString(stream, StandardCharsets.UTF_8);
                loadFromJsonString(registryName, json);
                return true;
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                CardduelMod.LOGGER.warn(MARKER, "Failed to read data file: {}, entry: {}", zipFile, entry);
                exception.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 文件夹格式加载
     */
    public static void load(File root) {
        Path filePath = root.toPath().resolve("cards/data");
        if (Files.isDirectory(filePath)) {
            PathVisitor visitor = new PathVisitor(filePath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    String json = IOUtils.toString(stream, StandardCharsets.UTF_8);
                    loadFromJsonString(id, json);
                } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                    CardduelMod.LOGGER.warn(MARKER, "Failed to read data file: {}", file);
                    exception.printStackTrace();
                }
            });
            try {
                Files.walkFileTree(filePath, visitor);
            } catch (IOException e) {
                CardduelMod.LOGGER.warn(MARKER, "Failed to walk file tree: {}", filePath);
                e.printStackTrace();
            }
        }
    }

    public static void loadFromJsonString(ResourceLocation id, String json) {
        CardDataPOJO data = CommonCardPackLoader.GSON.fromJson(json, CardDataPOJO.class);
        CardAssetManager.INSTANCE.putCardData(id, data);
    }

}
