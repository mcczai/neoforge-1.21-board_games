package net.mcczai.cardduel.resources.data;

import com.google.gson.annotations.SerializedName;

public class CardData {

    @SerializedName("hp")
    private int HP = 1;

    @SerializedName("mp")
    private int MP = 1;

    @SerializedName("atk")
    private int ATK = 1;

    @SerializedName("description")
    private String DESCRIPTION = null;

    @SerializedName("type")
    private int TYPE = 0;

    @SerializedName("skill")
    private String SKILL = null;

    public int getHP(){
        return HP;
    }

    public int getMP(){
        return MP;
    }

    public int getATK(){
        return ATK;
    }

    public String getDESCRIPTION(){
        return DESCRIPTION;
    }

    public int getTYPE(){
        return TYPE;
    }

    public String getSKILL(){
        return SKILL;
    }

}
