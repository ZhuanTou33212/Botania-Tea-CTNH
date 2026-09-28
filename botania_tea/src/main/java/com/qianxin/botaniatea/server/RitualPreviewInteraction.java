package com.qianxin.botaniatea.server;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.registry.ModItems;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vazkii.botania.common.block.BotaniaBlocks;

@Mod.EventBusSubscriber(modid=BotaniaTeaMod.MOD_ID)
public final class RitualPreviewInteraction {
    public static boolean matches(PlayerInteractEvent.RightClickBlock event) {
        return event.getEntity().isShiftKeyDown() && event.getItemStack().is(ModItems.MANA_OFFERING.get())
                && event.getLevel().getBlockState(event.getPos()).is(BotaniaBlocks.runeAltar);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!matches(event)) return;
        // Consume the click on both sides: do not insert into the altar or place a pedestal by accident.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
    private RitualPreviewInteraction() {}
}
