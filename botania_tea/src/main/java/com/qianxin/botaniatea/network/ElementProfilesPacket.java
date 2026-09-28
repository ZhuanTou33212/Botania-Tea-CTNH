package com.qianxin.botaniatea.network;

import com.qianxin.botaniatea.content.ElementAnalyzer;
import com.qianxin.botaniatea.content.ElementType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;
import java.util.function.Supplier;

/** Server-authored, bounded display-only snapshot; no client recipe or cost authority. */
public record ElementProfilesPacket(boolean reset, Map<ResourceLocation, Map<ElementType, String>> entries) {
    public static void send(ServerPlayer player) {
        Map<ResourceLocation, Map<ElementType, String>> page = new LinkedHashMap<>();
        boolean first = true;
        for (var profile : ElementAnalyzer.snapshot().values()) {
            if (profile.vector() == null) continue;
            Map<ElementType,String> values = new EnumMap<>(ElementType.class);
            profile.vector().values().forEach((type, amount) -> values.put(type, amount.toString()));
            page.put(profile.itemId(), values);
            if (page.size() == 128) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ElementProfilesPacket(first, page));
                first = false; page = new LinkedHashMap<>();
            }
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ElementProfilesPacket(first, page));
    }
    public static void encode(ElementProfilesPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.reset); buf.writeVarInt(packet.entries.size());
        packet.entries.forEach((id, values) -> {
            buf.writeResourceLocation(id); buf.writeVarInt(values.size());
            values.forEach((type, amount) -> { buf.writeEnum(type); buf.writeUtf(amount, 256); });
        });
    }
    public static ElementProfilesPacket decode(FriendlyByteBuf buf) {
        boolean reset = buf.readBoolean(); int size = buf.readVarInt();
        if (size < 0 || size > 128) throw new IllegalArgumentException("Invalid element page");
        Map<ResourceLocation,Map<ElementType,String>> entries = new LinkedHashMap<>();
        for (int i=0;i<size;i++) {
            var id=buf.readResourceLocation(); int count=buf.readVarInt();
            if (count<0 || count>ElementType.values().length) throw new IllegalArgumentException("Invalid element count");
            Map<ElementType,String> values=new EnumMap<>(ElementType.class);
            for (int j=0;j<count;j++) values.put(buf.readEnum(ElementType.class),buf.readUtf(256));
            entries.put(id,Map.copyOf(values));
        }
        return new ElementProfilesPacket(reset,Map.copyOf(entries));
    }
    public static void handle(ElementProfilesPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.qianxin.botaniatea.client.ElementTooltip.accept(packet)));
        context.get().setPacketHandled(true);
    }
}
