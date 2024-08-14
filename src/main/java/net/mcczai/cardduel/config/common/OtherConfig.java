package net.mcczai.cardduel.config.common;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForgeConfig;

public class OtherConfig {
    public static ModConfigSpec.BooleanValue DEFAULT_PACK_DEBUG;

    public static void init(ModConfigSpec.Builder builder){
        builder.push("other");

        builder.comment("When enabled, the reload command will not overwrite the default model file under config");
        DEFAULT_PACK_DEBUG = builder.define("DefaultPackDebug",false);
    }
}
