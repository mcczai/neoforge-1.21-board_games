package net.mcczai.cardduel.resources.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class CardData {

    @SerializedName("hp")
    private int HP = 1;

    @SerializedName("atk")
    private int ATK = 1;


    public int getHP(){
        return HP;
    }

    public int getATK(){
        return ATK;
    }
}
