package net.mcczai.cardduel.init;

import com.mojang.serialization.Codec;
import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.item.component.ItemStackWrapper;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(CardduelMod.MODID);

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<ItemStackWrapper>> IN_CARD = DATA_COMPONENTS.registerComponentType(
            "in_card",builder -> builder.persistent(ItemStackWrapper.CODEC).networkSynchronized(ItemStackWrapper.STREAM_CODEC).cacheEncoding()
    );

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> HP = DATA_COMPONENTS.registerComponentType(
            "hp",builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
    );

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> ATK = DATA_COMPONENTS.registerComponentType(
            "atk",builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
    );

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<String>> DESCRIPTION = DATA_COMPONENTS.registerComponentType(
            "description",builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8)
    );

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> TYPE = DATA_COMPONENTS.registerComponentType(
            "type",builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
    );

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<String>> SKILL = DATA_COMPONENTS.registerComponentType(
            "skill",builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8)
    );
}
