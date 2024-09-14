package net.mcczai.cardduel.resources.loader;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.resources.CommonCardIndex;
import net.mcczai.cardduel.resources.pojo.CardIndexPOJO;
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

import static net.mcczai.cardduel.resources.CommonCardPackLoader.CARD_INDEX;
import static net.mcczai.cardduel.resources.CommonCardPackLoader.GSON;

public class CommonCardIndexLoader {
    private static final Pattern CARD_INDEX_PATTERN = Pattern.compile("^(\\w+)/guns/index/(\\w+)\\.json$");
    private static final Marker MARKER = MarkerManager.getMarker("CommonGunIndexLoader");

    public static void loadCardIndex(String path, ZipFile zipFile) throws IOException {
        Matcher matcher = CARD_INDEX_PATTERN.matcher(path);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            String id = matcher.group(2);
            ZipEntry entry = zipFile.getEntry(path);
            if (entry == null) {
                CardduelMod.LOGGER.warn(MARKER, "{} file don't exist", path);
                return;
            }
            try (InputStream stream = zipFile.getInputStream(entry)) {
                String json = IOUtils.toString(stream, StandardCharsets.UTF_8);
                ResourceLocation registryName =  ResourceLocation.fromNamespaceAndPath(namespace, id);
                loadCardFromJsonString(registryName, json);
            } catch (IllegalArgumentException | JsonSyntaxException | JsonIOException exception) {
                CardduelMod.LOGGER.warn("{} index file read fail!", path);
                exception.printStackTrace();
            }
        }
    }

    public static void loadCardIndex(File root) throws IOException {
        Path filePath = root.toPath().resolve("guns/index");
        if (Files.isDirectory(filePath)) {
            PathVisitor visitor = new PathVisitor(filePath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    String json = IOUtils.toString(stream, StandardCharsets.UTF_8);
                    loadCardFromJsonString(id, json);
                } catch (IllegalArgumentException | IOException | JsonSyntaxException | JsonIOException exception) {
                    CardduelMod.LOGGER.warn("{} index file read fail!", file);
                    exception.printStackTrace();
                }
            });
            Files.walkFileTree(filePath, visitor);
        }
    }

    public static void loadCardFromJsonString(ResourceLocation id, String json) {
        CardIndexPOJO indexPOJO = GSON.fromJson(json, CardIndexPOJO.class);
        CARD_INDEX.put(id, CommonCardIndex.getInstance(indexPOJO));
    }
}
