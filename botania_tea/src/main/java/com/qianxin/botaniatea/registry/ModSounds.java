package com.qianxin.botaniatea.registry;

import com.qianxin.botaniatea.BotaniaTeaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Resource-pack replaceable events; the default sounds.json explicitly reuses vanilla audio. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BotaniaTeaMod.MOD_ID);
    public static final RegistryObject<SoundEvent> SEAL_APPLIED = register("seal_applied");
    public static final RegistryObject<SoundEvent> RITUAL_SUMMON = register("ritual_summon");
    public static final RegistryObject<SoundEvent> BOSS_WINDUP = register("boss_windup");
    public static final RegistryObject<SoundEvent> BOSS_DEFEAT = register("boss_defeat");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(BotaniaTeaMod.MOD_ID, name)));
    }

    private ModSounds() {}
}
