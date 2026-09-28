package com.qianxin.botaniatea.server;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import com.qianxin.botaniatea.content.TeaFlower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BotaniaTeaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BuildingSelectionEvents {
    private BuildingSelectionEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void select(PlayerInteractEvent.RightClickBlock event) {
        if (BuildingSelectionService.checkingPermission() || !(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()
                || !player.getOffhandItem().isEmpty() || event.getUseBlock() == Event.Result.DENY || event.getUseItem() == Event.Result.DENY) return;
        // Flower clicks belong to TeaSpecialFlowerBlock.use so they also work with normal block interaction.
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof RegisteredFunctionalFlowerBlockEntity flower
                && flower.flower() == TeaFlower.BUILDING_BEGONIA) return;
        if (BuildingSelectionService.select(player, event.getHitVec())) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.CONSUME);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) BuildingSelectionService.tick(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { BuildingSelectionService.close(event.getEntity().getUUID()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { BuildingSelectionService.close(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { BuildingSelectionService.clear(); }
}
