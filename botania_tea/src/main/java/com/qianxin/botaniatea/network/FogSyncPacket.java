package com.qianxin.botaniatea.network;

import com.qianxin.botaniatea.client.ClientFogState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record FogSyncPacket(UUID id, BlockPos center, int remaining, boolean active) {
    public static void encode(FogSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.id);
        buffer.writeBlockPos(packet.center);
        buffer.writeVarInt(packet.remaining);
        buffer.writeBoolean(packet.active);
    }

    public static FogSyncPacket decode(FriendlyByteBuf buffer) {
        return new FogSyncPacket(buffer.readUUID(), buffer.readBlockPos(), buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(FogSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientFogState.apply(packet.id(), packet.center(), packet.remaining(), packet.active())));
        context.setPacketHandled(true);
    }
}
