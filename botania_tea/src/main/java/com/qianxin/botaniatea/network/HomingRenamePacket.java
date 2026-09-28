package com.qianxin.botaniatea.network;

import com.qianxin.botaniatea.server.HomingService;
import com.qianxin.botaniatea.world.HomingNetworkData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** The player can rename only the nearby flower they are configuring. */
public record HomingRenamePacket(ResourceLocation dimension, BlockPos source, int page, String name) {
    public static void encode(HomingRenamePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBlockPos(packet.source);
        buffer.writeVarInt(packet.page);
        buffer.writeUtf(packet.name, HomingNetworkData.MAX_NAME_LENGTH);
    }
    public static HomingRenamePacket decode(FriendlyByteBuf buffer) {
        return new HomingRenamePacket(buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readVarInt(),
                buffer.readUtf(HomingNetworkData.MAX_NAME_LENGTH));
    }
    public static void handle(HomingRenamePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            var player = context.get().getSender();
            if (player == null || !HomingService.canConfigure(player, packet.dimension, packet.source)) return;
            boolean saved = HomingService.rename(player, packet.dimension, packet.source, packet.name);
            HomingService.sendOptions(player, packet.dimension, packet.source, packet.page,
                    saved ? "名称已保存" : "名称不合法（最多32字符，不含控制符）");
        });
        context.get().setPacketHandled(true);
    }
}
