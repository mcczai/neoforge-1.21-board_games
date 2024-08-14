package net.mcczai.cardduel.API.resource;

import com.google.common.collect.Lists;

import java.nio.file.Paths;
import java.util.List;

public final class ResourceManager {

    public static final List<ExtraEntry> EXTRA_ENTRIES = Lists.newArrayList();

    /**
     * 注册待解压的包
     * @param modMainClass      主类
     * @param extraFolderPath   需要进行解压的文件夹路径
     */
    public static void registerExtraGunPack(Class<?> modMainClass, String extraFolderPath) {
        EXTRA_ENTRIES.add(new ExtraEntry(modMainClass, extraFolderPath, Paths.get(extraFolderPath).getFileName().toString()));
    }

    /**
     *  解压条目
     */
    public record ExtraEntry(Class<?> modMainClass,String srcPath,String extraDirName){
    }
}
