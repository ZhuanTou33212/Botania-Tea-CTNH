package com.qianxin.botaniatea.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** One server-selected page: the registry itself is never truncated to a packet-sized list. */
public record HomingOptionsPacket(ResourceLocation dimension, BlockPos source, @Nullable BlockPos selected,
                                  int page, int pages, List<Option> options, String status,
                                  String sourceName, String selectedName, String readiness) {
    public static final int PAGE_SIZE = 5;
    public record Option(BlockPos pos, boolean available, String name) {}

    public HomingOptionsPacket { options = List.copyOf(options); }

    public static void encode(HomingOptionsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBlockPos(packet.source);
        buffer.writeBoolean(packet.selected != null);
        if (packet.selected != null) buffer.writeBlockPos(packet.selected);
        buffer.writeVarInt(packet.page);
        buffer.writeVarInt(packet.pages);
        buffer.writeVarInt(packet.options.size());
        for (Option option : packet.options) {
            buffer.writeBlockPos(option.pos);
            buffer.writeBoolean(option.available);
            buffer.writeUtf(option.name, 32);
        }
        buffer.writeUtf(packet.status, 128);
        buffer.writeUtf(packet.sourceName, 32);
        buffer.writeUtf(packet.selectedName, 32);
        buffer.writeUtf(packet.readiness, 128);
    }

    public static HomingOptionsPacket decode(FriendlyByteBuf buffer) {
        var dimension = buffer.readResourceLocation();
        var source = buffer.readBlockPos();
        BlockPos selected = buffer.readBoolean() ? buffer.readBlockPos() : null;
        int page = buffer.readVarInt(), pages = buffer.readVarInt(), count = buffer.readVarInt();
        if (page < 0 || pages < 1 || page >= pages || count < 0 || count > PAGE_SIZE)
            throw new IllegalArgumentException("Invalid homing options page");
        var options = new ArrayList<Option>(count);
        for (int i = 0; i < count; i++) options.add(new Option(buffer.readBlockPos(), buffer.readBoolean(), buffer.readUtf(32)));
        return new HomingOptionsPacket(dimension, source, selected, page, pages, options, buffer.readUtf(128),
                buffer.readUtf(32), buffer.readUtf(32), buffer.readUtf(128));
    }

    public static void handle(HomingOptionsPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.qianxin.botaniatea.client.HomingSelectionScreen.open(packet)));
        context.get().setPacketHandled(true);
    }
}
