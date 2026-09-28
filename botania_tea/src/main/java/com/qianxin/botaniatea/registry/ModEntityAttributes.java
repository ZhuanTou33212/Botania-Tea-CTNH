package com.qianxin.botaniatea.registry;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.entity.TeaMountainSpiritEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BotaniaTeaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributes {
    private ModEntityAttributes() {}

    @SubscribeEvent
    public static void register(EntityAttributeCreationEvent event) {
        event.put(ModEntities.TEA_MOUNTAIN_SPIRIT.get(), TeaMountainSpiritEntity.createAttributes().build());
    }
}
