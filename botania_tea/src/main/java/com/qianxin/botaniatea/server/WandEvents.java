package com.qianxin.botaniatea.server;

import com.qianxin.botaniatea.BotaniaTeaMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BotaniaTeaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WandEvents {
    private WandEvents() {}
    @SubscribeEvent public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) WandService.clearTransient(p); }
    @SubscribeEvent public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) { if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) WandService.clearTransient(p); }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        WandService.handleDamage(event);
        WandService.handleBranchDamage(event);
    }
    @SubscribeEvent public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer() != null) WandService.tick(event.getServer().overworld().getGameTime(), event.getServer().getAllLevels());
    }
    @SubscribeEvent public static void onClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        net.minecraft.nbt.CompoundTag old = event.getOriginal().getPersistentData();
        net.minecraft.nbt.CompoundTag next = event.getEntity().getPersistentData();
        if (old.contains("BotaniaTeaWandCooldowns")) next.put("BotaniaTeaWandCooldowns", old.getCompound("BotaniaTeaWandCooldowns").copy());
        if (!event.isWasDeath() && old.contains("BotaniaTeaAftertasteShield")) next.putFloat("BotaniaTeaAftertasteShield", old.getFloat("BotaniaTeaAftertasteShield"));
    }
}
