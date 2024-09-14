package net.mcczai.cardduel.client.resource.loader.asset;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.client.resource.ClientAssetManager;
import net.mcczai.cardduel.client.resource.pojo.PackInfoPOJO;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.mcczai.cardduel.client.resource.ClientCardPackLoader.GSON;

public class PackInfoLoader {
    private static final Marker MARKER = MarkerManager.getMarker("CreativeTabLoader");
    private static final Pattern PACK_INFO_PATTERN = Pattern.compile("^(\\w+)/pack\\.json$");

    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher matcher = PACK_INFO_PATTERN.matcher(zipPath);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            ZipEntry entry = zipFile.getEntry(zipPath);
            if (entry == null) {
                CardduelMod.LOGGER.warn(MARKER, "{} file don't exist", zipPath);
                return false;
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                PackInfoPOJO packInfoPOJO = GSON.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), PackInfoPOJO.class);
                ClientAssetManager.INSTANCE.putPackInfo(namespace, packInfoPOJO);
                return true;
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                CardduelMod.LOGGER.warn(MARKER, "Failed to read info json: {}, entry: {}", zipFile, entry);
                exception.printStackTrace();
            }
        }
        return false;
    }

    public static void load(@NotNull File root) {
        Path packInfoFilePath = root.toPath().resolve("pack.json");
        if (Files.isRegularFile(packInfoFilePath)) {
            try (InputStream stream = Files.newInputStream(packInfoFilePath)) {
                PackInfoPOJO packInfoPOJO = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackInfoPOJO.class);
                ClientAssetManager.INSTANCE.putPackInfo(root.getName(), packInfoPOJO);
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                CardduelMod.LOGGER.warn(MARKER, "Failed to read info json: {}", packInfoFilePath);
                exception.printStackTrace();
            }
        }
    }

}
