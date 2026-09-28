package com.qianxin.botaniatea.network;

import com.qianxin.botaniatea.BotaniaTeaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "5";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(BotaniaTeaMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int messageId;

    public static void register() {
        CHANNEL.registerMessage(messageId++, ElementProfilesPacket.class, ElementProfilesPacket::encode,
                ElementProfilesPacket::decode, ElementProfilesPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, FogSyncPacket.class,
                FogSyncPacket::encode, FogSyncPacket::decode, FogSyncPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, DreamPacket.class, DreamPacket::encode, DreamPacket::decode,
                DreamPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, HomingOptionsPacket.class, HomingOptionsPacket::encode, HomingOptionsPacket::decode,
                HomingOptionsPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, HomingSelectionPacket.class, HomingSelectionPacket::encode, HomingSelectionPacket::decode,
                HomingSelectionPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(messageId++, HomingRenamePacket.class, HomingRenamePacket::encode, HomingRenamePacket::decode,
                HomingRenamePacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
    }

    private ModNetwork() {}
}
