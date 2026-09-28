package com.qianxin.botaniatea.content;

import com.qianxin.botaniatea.block.entity.ManaOfferingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;

/**
 * Any wand of the forest lights the circle once the player has built the livingrock base.
 *
 * <p>Using the altar also answers "why is nothing happening": the ritual itself refuses silently, so
 * the first unmet gate is reported in chat in the same order the ritual checks it.
 */
@Mod.EventBusSubscriber(modid="botania_tea")
public final class CircleActivation {
    private CircleActivation() {}

    /** Botania two wands of the forest; both are accepted. */
    private static final java.util.Set<ResourceLocation> WANDS = java.util.Set.of(
            ResourceLocation.fromNamespaceAndPath("botania","twig_wand"),
            ResourceLocation.fromNamespaceAndPath("botania","dreamwood_wand"));

    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;
        if (!level.getBlockState(event.getPos()).is(BotaniaBlocks.runeAltar)) return;
        var held = event.getEntity().getItemInHand(event.getHand());
        var id = ForgeRegistries.ITEMS.getKey(held.getItem());
        boolean wand = id != null && WANDS.contains(id);
        // An empty hand only asks for a status; any other item still belongs to the altar itself.
        if (!wand && !held.isEmpty()) return;
        var tables = RitualAssembly.tables(level, event.getPos());
        if (tables.isEmpty()) {
            say(event.getEntity(), "message.botania_tea.circle.no_tables");
        } else if (!RitualAssembly.foundationIntact(level, event.getPos())) {
            say(event.getEntity(), "message.botania_tea.circle.needs_livingrock");
        } else {
            if (wand) {
                for (var table : tables) if (!table.circleActive) { table.circleActive = true; table.setChanged(); }
                say(event.getEntity(), "message.botania_tea.circle.active");
            }
            report(event.getEntity(), level, event.getPos(), tables);
        }
        // Taking the item back off the altar stays Botania's own empty-hand behaviour.
        if (wand) event.setCanceled(true);
    }

    /** First unmet gate wins, so the player is told exactly one thing to fix. */
    private static void report(Player player, Level level, BlockPos altar, java.util.List<ManaOfferingBlockEntity> tables) {
        if (tables.stream().anyMatch(table -> !table.circleActive)) {
            say(player, "message.botania_tea.ritual.needs_wand");
            return;
        }
        if (level.getBlockEntity(altar) instanceof vazkii.botania.common.block.block_entity.RunicAltarBlockEntity runic
                && runic.getItemHandler().getItem(0).isEmpty()) {
            say(player, "message.botania_tea.ritual.needs_offering");
            return;
        }
        if (!level.hasNeighborSignal(altar)) {
            say(player, "message.botania_tea.ritual.needs_redstone");
            return;
        }
        ManaPoolBlockEntity pool = poolNear(level, altar);
        if (pool == null) {
            say(player, "message.botania_tea.ritual.no_pool");
            return;
        }
        boolean named = !level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ItemFrame.class,
                new net.minecraft.world.phys.AABB(altar).inflate(2),
                frame -> frame.getPos().relative(frame.getDirection().getOpposite()).equals(altar)
                        && !frame.getItem().isEmpty()).isEmpty();
        player.displayClientMessage(Component.translatable("message.botania_tea.ritual.status"), false);
        if (!named) say(player, "message.botania_tea.ritual.hint_frame");
    }

    /** The mana pool the ritual would bind to: one that sits inside the altar's own range. */
    private static ManaPoolBlockEntity poolNear(Level level, BlockPos altar) {
        for (var p : BlockPos.betweenClosed(altar.offset(-2,-1,-2), altar.offset(2,1,2))) {
            if (RitualAssembly.inPoolRange(altar, p)
                    && level.getBlockEntity(p) instanceof ManaPoolBlockEntity pool) return pool;
        }
        return null;
    }

    private static void say(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), false);
    }
}
