package net.mcczai.cardduel.network.payload;

import net.mcczai.cardduel.CardduelMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：认输（换牌阶段与对局中均可）。
 */
public record ServerboundSurrenderPayload() implements CustomPacketPayload {

    public static final Type<ServerboundSurrenderPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CardduelMod.MODID, "duel_surrender"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSurrenderPayload> STREAM_CODEC =
            StreamCodec.unit(new ServerboundSurrenderPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
