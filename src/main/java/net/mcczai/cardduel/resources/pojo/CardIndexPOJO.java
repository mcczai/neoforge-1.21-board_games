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
}
