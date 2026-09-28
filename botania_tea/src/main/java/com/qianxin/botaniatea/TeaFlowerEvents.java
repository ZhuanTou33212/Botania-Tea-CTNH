package com.qianxin.botaniatea;

import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import com.qianxin.botaniatea.content.TeaFlower;
import com.qianxin.botaniatea.world.TeaWorldEventData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import vazkii.botania.api.mana.ManaItemHandler;
import com.qianxin.botaniatea.item.PorcelainGuardItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side area hooks for the two drop-protection rules owned by guardian lotus. */
@Mod.EventBusSubscriber(modid = BotaniaTeaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TeaFlowerEvents {
    private TeaFlowerEvents() {}

    @SubscribeEvent
    public static void onBinding(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        if (!player.isShiftKeyDown() || !event.getItemStack().is(vazkii.botania.common.item.BotaniaItems.twigWand)) return;
        if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(event.getPos())) > 64) return;
        if (event.getLevel().isClientSide) return;
        var itemTag = event.getItemStack().getOrCreateTag();
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof com.qianxin.botaniatea.block.entity.RegisteredGeneratingFlowerBlockEntity flower
                && TeaFlower.fromBlock(flower.getBlockState().getBlock()) == TeaFlower.AFTERTASTE_DAISY && itemTag.contains("TeaEndpoint")) {
            var endpoint = itemTag.getCompound("TeaEndpoint");
            if (flower.bindResidue(BlockPos.of(endpoint.getLong("Pos")), net.minecraft.core.Direction.from3DDataValue(endpoint.getInt("Side")), endpoint.getString("Dimension"))) {
                itemTag.remove("TeaEndpoint"); flower.sync();
                event.setCanceled(true); event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
            }
            return;
        }
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof RegisteredFunctionalFlowerBlockEntity flower) {
            if (flower.flower() != TeaFlower.BUILDING_BEGONIA && flower.flower() != TeaFlower.HOMING_MORNING_GLORY
                    && flower.flower() != TeaFlower.GAUGING_HYDRANGEA && flower.flower() != TeaFlower.MENDING_HIBISCUS) return;
            if (!itemTag.contains("TeaEndpoint")) return; // let Botania handle its normal mana binding
            var endpoint = itemTag.getCompound("TeaEndpoint");
            boolean bound = flower.extraWork().bind(BlockPos.of(endpoint.getLong("Pos")),
                    net.minecraft.core.Direction.from3DDataValue(endpoint.getInt("Side")), endpoint.getString("Dimension"));
            if (bound) { itemTag.remove("TeaEndpoint"); flower.sync(); }
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(bound ? "茶花绑定已保存" : "目标未加载、维度或距离不合法"), true);
            event.setCanceled(true); event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
        } else {
            if (event.getLevel().getBlockEntity(event.getPos()) instanceof vazkii.botania.api.mana.ManaPool) {
                itemTag.remove("TeaEndpoint");
                return;
            }
            // Remember a separate endpoint without canceling Botania's own pool/wand handling.
            var endpoint = new net.minecraft.nbt.CompoundTag();
            endpoint.putString("Dimension", event.getLevel().dimension().location().toString());
            endpoint.putLong("Pos", event.getPos().asLong());
            endpoint.putInt("Side", (event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace()).ordinal());
            itemTag.put("TeaEndpoint", endpoint);
        }
    }

    @SubscribeEvent
    public static void onNaturalSpawn(net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() == net.minecraft.world.entity.MobSpawnType.NATURAL
                && event.getEntity().getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER
                && com.qianxin.botaniatea.server.TeaAreaIndex.contains(event.getEntity().level(), event.getEntity().position(), TeaFlower.NIGHT_EPIPHYLLUM)) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onBuilderPlace(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof net.minecraftforge.common.util.FakePlayer player)
                || !player.getGameProfile().getName().equals("[TeaBuilder]")) return;
        BlockPos origin = player.blockPosition();
        java.util.List<BlockPos> positions = event instanceof net.minecraftforge.event.level.BlockEvent.EntityMultiPlaceEvent multi
                ? multi.getReplacedBlockSnapshots().stream().map(net.minecraftforge.common.util.BlockSnapshot::getPos).toList()
                : java.util.List.of(event.getPos());
        if (positions.stream().anyMatch(pos -> pos.distSqr(origin) > 64 || Math.abs(pos.getY() - origin.getY()) > 2
                || !player.level().hasChunkAt(pos) || !player.level().getWorldBorder().isWithinBounds(pos))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        // Player-accessible configuration, still constrained to a nearby loaded flower on the server.
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("botaniatea")
                .then(net.minecraft.commands.Commands.literal("measure")
                .then(net.minecraft.commands.Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                .then(net.minecraft.commands.Commands.argument("fluid", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                .then(net.minecraft.commands.Commands.argument("less", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                .then(net.minecraft.commands.Commands.argument("threshold", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(context, "pos");
                    if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 64 || !player.level().hasChunkAt(pos)
                            || !(player.level().getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower)
                            || flower.flower() != TeaFlower.GAUGING_HYDRANGEA) return 0;
                    flower.extraWork().configureMeasure(com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "fluid"),
                            com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "less"),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "threshold"));
                    flower.sync();
                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(flower.extraWork().summary()), false);
                    return 1;
                })))))));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) return;
        for (net.minecraft.server.level.ServerLevel level : event.getServer().getAllLevels()) {
            TeaWorldEventData.get(level).tick(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            TeaWorldEventData.get(player.serverLevel()).syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) TeaWorldEventData.get(player.serverLevel()).syncTo(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) TeaWorldEventData.get(player.serverLevel()).syncTo(player);
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        ItemEntity item = event.getItem();
        if (com.qianxin.botaniatea.server.TeaAreaIndex.contains(item.level(), item.position(), TeaFlower.GUARDIAN_LOTUS)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Entity source = event.getExplosion().getDirectSourceEntity();
        if (!(source instanceof PrimedTnt)) return;
        event.getAffectedEntities().removeIf(entity -> entity instanceof ItemEntity item
                && com.qianxin.botaniatea.server.TeaAreaIndex.contains(item.level(), item.position(), TeaFlower.GUARDIAN_LOTUS));
    }

    @SubscribeEvent
    public static void onGuard(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)
                || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof PorcelainGuardItem)
                || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        Entity source = event.getSource().getEntity();
        if (source == null) return;
        net.minecraft.world.phys.Vec3 toSource = source.position().subtract(player.position()).normalize();
        double front = player.getViewVector(1.0F).dot(toSource);
        if (front < 0.5D) return;
        int unitCost = com.qianxin.botaniatea.config.TeaGardenConfig.GUARD_COST.get();
        int mana = Math.max(0, (int) Math.ceil(event.getAmount() * .5F * unitCost));
        int paid = ManaItemHandler.instance().requestManaForTool(player.getUseItem(), player, mana, true);
        if (paid > 0) {
            float reduced = Math.min(event.getAmount() * .5F, (float) paid / unitCost);
            event.setAmount(event.getAmount() - reduced);
            player.getUseItem().hurtAndBreak(Math.max(1, (int) Math.ceil(reduced)), player,
                    p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }
    }

    @SubscribeEvent
    public static void onDrinkStart(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start event) {
        if (event.getEntity().getOffhandItem().getItem() instanceof PorcelainGuardItem
                && (event.getItem().getItem() instanceof com.qianxin.teaart.item.TeaDrinkItem
                || event.getItem().getItem() instanceof com.qianxin.botaniatea.item.AscendedTeaBottleItem
                || event.getItem().getItem() instanceof com.qianxin.botaniatea.item.TravelTeapotItem)) {
            event.setDuration(Math.max(1, event.getDuration() / 2));
        }
    }

}
