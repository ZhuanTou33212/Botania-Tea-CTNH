package com.qianxin.botaniatea.client;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.block.entity.TeaEssenceStillbloomBlockEntity;
import com.qianxin.botaniatea.block.entity.SublimationBloomBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vazkii.botania.api.BotaniaForgeClientCapabilities;
import vazkii.botania.api.block_entity.BindableSpecialFlowerBlockEntity;
import vazkii.botania.forge.CapabilityUtil;

@Mod.EventBusSubscriber(modid = BotaniaTeaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private static final ResourceLocation WAND_HUD_ID = ResourceLocation.fromNamespaceAndPath(BotaniaTeaMod.MOD_ID, "wand_hud");

    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void attachBlockEntityCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof TeaEssenceStillbloomBlockEntity flower) {
            event.addCapability(WAND_HUD_ID, CapabilityUtil.makeProvider(
                    BotaniaForgeClientCapabilities.WAND_HUD,
                    new BindableSpecialFlowerBlockEntity.BindableFlowerWandHud<>(flower)));
        } else if (event.getObject() instanceof SublimationBloomBlockEntity flower) {
            event.addCapability(WAND_HUD_ID, CapabilityUtil.makeProvider(
                    BotaniaForgeClientCapabilities.WAND_HUD,
                    new BindableSpecialFlowerBlockEntity.BindableFlowerWandHud<>(flower)));
        } else if (event.getObject() instanceof com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity flower) {
            event.addCapability(WAND_HUD_ID, CapabilityUtil.makeProvider(
                    BotaniaForgeClientCapabilities.WAND_HUD,
                    new BindableSpecialFlowerBlockEntity.BindableFlowerWandHud<>(flower)));
        } else if (event.getObject() instanceof com.qianxin.botaniatea.block.entity.RegisteredGeneratingFlowerBlockEntity flower) {
            event.addCapability(WAND_HUD_ID, CapabilityUtil.makeProvider(
                    BotaniaForgeClientCapabilities.WAND_HUD,
                    new BindableSpecialFlowerBlockEntity.BindableFlowerWandHud<>(flower)));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) { ClientFogState.tick(); ClientDreamState.tick(); }
    }

    @SubscribeEvent
    public static void onDreamOverlay(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
        ClientDreamState.render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onFogVolume(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        ClientFogState.render(event);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && ClientFogState.contains(player.blockPosition())) {
            event.setRed(.28F); event.setGreen(.25F); event.setBlue(.32F);
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player != null && ClientFogState.contains(minecraft.player.blockPosition())) {
            event.setNearPlaneDistance(0.0F);
            event.setFarPlaneDistance(2.0F);
            event.setCanceled(true);
        }
    }
}
