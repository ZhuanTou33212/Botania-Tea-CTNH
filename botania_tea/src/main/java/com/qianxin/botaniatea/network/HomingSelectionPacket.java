package com.qianxin.botaniatea.network;

import com.qianxin.botaniatea.server.HomingService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** select=false requests a page; select=true selects a flower (null clears the selection). */
public record HomingSelectionPacket(ResourceLocation dimension, BlockPos source, int page,
                                    boolean select, @Nullable BlockPos target) {
    public static void encode(HomingSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBlockPos(packet.source);
        buffer.writeVarInt(packet.page);
        buffer.writeBoolean(packet.select);
        buffer.writeBoolean(packet.target != null);
        if (packet.target != null) buffer.writeBlockPos(packet.target);
    }

    public static HomingSelectionPacket decode(FriendlyByteBuf buffer) {
        return new HomingSelectionPacket(buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBoolean() ? buffer.readBlockPos() : null);
    }

    public static void handle(HomingSelectionPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            var player = context.get().getSender();
            if (player == null || !HomingService.canConfigure(player, packet.dimension, packet.source)) return;
            String status = "";
            if (packet.select) status = HomingService.selectDestination(player, packet.dimension, packet.source, packet.target)
                    ? "目标已保存" : "目标失效、未加载或不合法，请重新选择";
            HomingService.sendOptions(player, packet.dimension, packet.source, packet.page, status);
        });
        context.get().setPacketHandled(true);
    }
}
