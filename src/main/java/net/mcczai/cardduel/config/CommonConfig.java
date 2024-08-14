package net.mcczai.cardduel.config;

import net.mcczai.cardduel.config.common.OtherConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class CommonConfig {
    public static ModConfigSpec init(){
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        OtherConfig.init(builder);
        return builder.build();
    }
}
