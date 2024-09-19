package net.mcczai.cardduel.items.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CardDataComponent(int atk, int hp, int mp, String type
        ,String skill1 ,String skill2 ,String skill3 ,String skill4 ,String skill5) {


    public static final Codec<CardDataComponent> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("atk").forGetter(o -> o.atk),
            Codec.INT.fieldOf("hp").forGetter(o -> o.hp),
            Codec.INT.fieldOf("mp").forGetter(o -> o.mp),
            Codec.STRING.fieldOf("type").forGetter(o -> o.type),
            Codec.STRING.fieldOf("skill1").forGetter(o -> o.skill1),
            Codec.STRING.fieldOf("skill2").forGetter(o -> o.skill2),
            Codec.STRING.fieldOf("skill3").forGetter(o -> o.skill3),
            Codec.STRING.fieldOf("skill4").forGetter(o -> o.skill4),
            Codec.STRING.fieldOf("skill5").forGetter(o -> o.skill5)
    ).apply(ins, CardDataComponent::new));


}
