package net.mcczai.cardduel.client.resource.loader.asset;

import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.client.resource.texture.FilePackTexture;
import net.mcczai.cardduel.client.resource.texture.ZipPackTexture;
import net.mcczai.cardduel.util.PathVisitor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TextureLoader {
    private static final Marker MARKER = MarkerManager.getMarker("TextureLoader");
    private static final Pattern TEXTURE_PATTERN = Pattern.compile("^(\\w+)/textures/([\\w/]+)\\.png$");

    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher matcher = TEXTURE_PATTERN.matcher(zipPath);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            String path = matcher.group(2);
            ZipEntry entry = zipFile.getEntry(zipPath);
            if (entry == null) {
                CardduelMod.LOGGER.warn(MARKER, "{} file don't exist", zipPath);
                return false;
            }
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
            ZipPackTexture zipPackTexture = new ZipPackTexture(id, zipFile.getName());
            Minecraft.getInstance().getTextureManager().register(id, zipPackTexture);
            return true;
        }
        return false;
    }

    public static void load(File root) {
        Path filePath = root.toPath().resolve("textures");
        if (Files.isDirectory(filePath)) {
            PathVisitor visitor = new PathVisitor(filePath.toFile(), root.getName(), ".png", (id, file) -> {
                FilePackTexture filePackTexture = new FilePackTexture(id, file);
                Minecraft.getInstance().getTextureManager().register(id, filePackTexture);
            });
            try {
                Files.walkFileTree(filePath, visitor);
            } catch (Exception e) {
                CardduelMod.LOGGER.warn(MARKER, "Failed to walk file tree: {}", filePath);
                e.printStackTrace();
            }
        }
    }
}
