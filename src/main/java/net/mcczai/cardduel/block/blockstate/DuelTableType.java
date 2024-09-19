package net.mcczai.cardduel.block.blockstate;

import net.minecraft.util.StringRepresentable;

public enum DuelTableType implements StringRepresentable {
    SINGLE("single"),
    FORMER("former"),
    POST("post");

    private final String name;

    private DuelTableType(String name){
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public DuelTableType getOpposite(){
        DuelTableType i;
        switch (this.ordinal()){
            case 0 -> i = SINGLE;
            case 1 -> i = POST;
            case 2 -> i = FORMER;
            default ->  throw new MatchException((String)null, (Throwable)null);
        }

        return i;
    }
}
