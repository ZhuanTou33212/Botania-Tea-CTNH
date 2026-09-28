package com.qianxin.botaniatea.server;

import com.qianxin.botaniatea.BotaniaTeaMod;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BotaniaTeaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HomingEvents {
    private HomingEvents() {}

    @SubscribeEvent public static void livingTick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide) HomingService.updateArrival(event.getEntity());
    }
}
