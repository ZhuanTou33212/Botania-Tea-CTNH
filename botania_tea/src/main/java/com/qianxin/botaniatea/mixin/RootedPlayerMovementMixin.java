package com.qianxin.botaniatea.mixin;

import com.qianxin.botaniatea.registry.ModEffects;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class RootedPlayerMovementMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleMovePlayer", at=@At("HEAD"), cancellable=true)
    private void lockPlayerPosition(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl)(Object)this, player.serverLevel());
        if (!player.hasEffect(ModEffects.ROOTED.get())) return;
        float yaw=packet.getYRot(player.getYRot()), pitch=packet.getXRot(player.getXRot());
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) return;
        if (packet.hasPosition() && (packet.getX(player.getX())!=player.getX() || packet.getY(player.getY())>player.getY() || packet.getZ(player.getZ())!=player.getZ())) {
            player.connection.teleport(player.getX(),player.getY(),player.getZ(),yaw,pitch);
            ci.cancel();
        }
    }
}
