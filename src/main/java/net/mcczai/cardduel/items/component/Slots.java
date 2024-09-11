package net.mcczai.cardduel.items.component;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record Slots(List<Integer> unsortables, List<Pair<Integer, ItemStack>> memory)
{
    public static final Codec<Slots> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                            Codec.INT.listOf().fieldOf("unsortables").forGetter(Slots::unsortables),
                            Codec.mapPair(Codec.INT.fieldOf("index"), ItemStack.CODEC.fieldOf("item")).codec().listOf().fieldOf("memory").forGetter(Slots::memory))
                    .apply(instance, Slots::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, Slots> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), Slots::unsortables,
            ByteBufCodecs.fromCodecWithRegistries(Codec.mapPair(Codec.INT.fieldOf("index"), ItemStack.CODEC.fieldOf("item")).codec()).apply(ByteBufCodecs.list()), Slots::memory,
            Slots::new
    );

    public static Slots createDefault()
    {
        return new Slots(List.of(), List.of());
    }
}
