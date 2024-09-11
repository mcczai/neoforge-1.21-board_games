package net.mcczai.cardduel.client.resource;

import com.google.common.collect.Maps;
import net.mcczai.cardduel.client.resource.pojo.PackInfoPOJO;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public enum ClientAssetManager {
    INSTANCE;
    /**
     * 存储卡包信息
     */
    private final Map<String, PackInfoPOJO> customInfos = Maps.newHashMap();

    /**
     * 存储语言信息
     */
    private final Map<String,Map<String,String>> languages = Maps.newHashMap();

    public void putPackInfo(String namespace, PackInfoPOJO info) {
        customInfos.put(namespace, info);
    }

    public void putLanguage(String region, Map<String, String> lang) {
        Map<String, String> languageMaps = languages.getOrDefault(region, Maps.newHashMap());
        languageMaps.putAll(lang);
        languages.put(region, languageMaps);
    }

    public void clearAll(){
        this.customInfos.clear();
        this.languages.clear();
    }
}
