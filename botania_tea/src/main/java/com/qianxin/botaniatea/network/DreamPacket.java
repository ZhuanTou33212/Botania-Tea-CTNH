package com.qianxin.botaniatea.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Cosmetic sleep transition only; no client authority over time or player pose. */
public record DreamPacket(int ticks) {
    public static void encode(DreamPacket p, FriendlyByteBuf b) { b.writeVarInt(p.ticks); }
    public static DreamPacket decode(FriendlyByteBuf b) { return new DreamPacket(Math.max(1, Math.min(200, b.readVarInt()))); }
    public static void handle(DreamPacket p, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.qianxin.botaniatea.client.ClientDreamState.start(p.ticks)));
        context.get().setPacketHandled(true);
    }
}
