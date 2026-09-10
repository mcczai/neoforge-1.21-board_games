package net.mcczai.cardduel.resources.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class CardIndexPOJO {

    @SerializedName("name")
    private String name;

    @SerializedName("data")
    private ResourceLocation data;

    @SerializedName("stack_size")
    private int stackSize;

    @SerializedName("tooltip")
    @Nullable
    private String tooltip;

    @SerializedName("type")
    @Nullable
    private String type;

    /** 稀有度：common / uncommon / rare / epic（也接受中文：普通/罕见/稀有/传说），缺省视为 common */
    @SerializedName("rarity")
    @Nullable
    private String rarity;

    @SerializedName("texture")
    @Nullable
    private ResourceLocation texture;

    @Nullable
    public ResourceLocation getTexture() {
        return texture;
    }

    public String getName() {
        return name;
    }

    public int getStackSize() {
        return stackSize;
    }

    @Nullable
    public String getTooltip() {
        return tooltip;
    }

    public ResourceLocation getData() {
        return data;
    }

    public String getType() {
        return type;
    }

    /**
     * @return 稀有度 id；未填写时返回 "common"
     */
    public String getRarity() {
        return rarity != null ? rarity : "common";
    }
}
