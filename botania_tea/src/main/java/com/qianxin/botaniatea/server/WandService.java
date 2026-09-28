package com.qianxin.botaniatea.server;

import com.qianxin.botaniatea.item.TeaWandItem;
import com.qianxin.botaniatea.registry.ModEffects;
import com.qianxin.botaniatea.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import vazkii.botania.api.mana.ManaItemHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** All wand targets, mana, cooldowns and damage originate on the server. */
public final class WandService {
    private static final String COOLDOWNS = "BotaniaTeaWandCooldowns";
    private static final String SHIELD = "BotaniaTeaAftertasteShield";
    private static final Map<UUID, BranchLink> LINKS = new HashMap<>();
    private static final ThreadLocal<Boolean> PROPAGATING = ThreadLocal.withInitial(() -> false);
    private WandService() {}

    public static boolean tryUse(ItemStack stack, ServerPlayer player) {
        if (!(stack.getItem() instanceof TeaWandItem wand)) return false;
        return switch (wand.wandId()) {
            case "shadow_shift" -> shadowShift(stack, player);
            case "aftertaste_guard" -> aftertasteGuard(stack, player, player);
            case "branch_link" -> branchLink(stack, player);
            case "dream_bind" -> dreamBind(stack, player);
            default -> false;
        };
    }

    private static boolean shadowShift(ItemStack stack, ServerPlayer player) {
        if (player.isPassenger() || player.isSleeping() || onCooldown(player, "shadow_shift")) return false;
        Vec3 start = player.getEyePosition(1.0F), look = player.getViewVector(1.0F).normalize(), end = start.add(look.scale(30));
        if (!com.qianxin.botaniatea.entity.BambooGrapplingHookEntity.loadedRay(player.serverLevel(), start, end)) return false;
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 destination = (hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(.45)))
                .subtract(0, player.getEyeHeight(), 0);
        Vec3 entrance = player.position().add(look.scale(1));
        if (!safeDestination(player, entrance) || !safeDestination(player, destination) || !pay(stack, player, 200)) return false;
        var first = new com.qianxin.botaniatea.entity.ShadowPortalEntity(player.serverLevel(), entrance, destination,
                player.getUUID(), player.getYRot());
        var second = new com.qianxin.botaniatea.entity.ShadowPortalEntity(player.serverLevel(), destination, entrance,
                player.getUUID(), player.getYRot());
        player.serverLevel().addFreshEntity(first);
        player.serverLevel().addFreshEntity(second);
        setCooldown(player, "shadow_shift", 64);
        return true;
    }

    private static boolean aftertasteGuard(ItemStack stack, ServerPlayer player, LivingEntity target) {
        if (onCooldown(player, "aftertaste_guard") || !pay(stack, player, 1000)) return false;
        target.getPersistentData().putFloat(SHIELD, 40);
        setCooldown(player, "aftertaste_guard", 3000);
        return true;
    }

    public static boolean useAftertasteGuard(ItemStack stack, ServerPlayer player, boolean self) {
        LivingEntity target = self ? player : rayTarget(player, 16);
        return target != null && aftertasteGuard(stack, player, target);
    }

    private static boolean branchLink(ItemStack stack, ServerPlayer player) {
        if (player.isShiftKeyDown()) { LINKS.remove(player.getUUID()); return true; }
        if (onCooldown(player,"branch_link")) return false;
        LivingEntity target = rayTarget(player, 16);
        if (target == null || !target.isAlive()) return false;
        BranchLink existing = LINKS.get(player.getUUID());
        if (existing == null || existing.expiresAt < now(player)) {
            for (BranchLink link : LINKS.values()) if ((link.first.equals(target.getUUID()) || target.getUUID().equals(link.second)) && link.expiresAt >= now(player)) return false;
            LINKS.put(player.getUUID(), new BranchLink(target.getUUID(), null, now(player) + 200, player.level().dimension().location().toString()));
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.botania_tea.branch_first"), true);
            return true;
        }
        LivingEntity first = find(player.serverLevel(), existing.first);
        for (var entry:LINKS.entrySet()) if (!entry.getKey().equals(player.getUUID()) && (entry.getValue().first.equals(target.getUUID()) || target.getUUID().equals(entry.getValue().second))) return false;
        if (first == null || first.distanceTo(target) > 24 || existing.first.equals(target.getUUID()) || onCooldown(player, "branch_link") || !pay(stack, player, 300)) return false;
        LINKS.put(player.getUUID(), new BranchLink(existing.first, target.getUUID(), now(player) + 200, player.level().dimension().location().toString()));
        setCooldown(player, "branch_link", 200);
        return true;
    }

    private static boolean dreamBind(ItemStack stack, ServerPlayer player) {
        LivingEntity target = rayTarget(player, 16);
        var effect=new MobEffectInstance(ModEffects.ROOTED.get(),100,0,true,false,true);
        if (target == null || !target.canBeAffected(effect) || onCooldown(player, "dream_bind") || !pay(stack, player, 250)) return false;
        target.addEffect(effect);
        setCooldown(player, "dream_bind", 200);
        return true;
    }

    public static boolean startBeam(ItemStack stack, ServerPlayer player) {
        if (onCooldown(player,"ember_beam") || !ManaItemHandler.instance().requestManaExactForTool(stack,player,20,false)) return false;
        player.getPersistentData().putInt("BotaniaTeaBeamTick",0);
        return true;
    }
    public static void stopBeam(ServerPlayer player) {
        player.getPersistentData().remove("BotaniaTeaBeamTick");
        setCooldown(player,"ember_beam",10);
    }
    public static void clearTransient(ServerPlayer player) {
        player.getPersistentData().remove(SHIELD);
        stopBeam(player);
        LINKS.entrySet().removeIf(e->e.getKey().equals(player.getUUID()) || e.getValue().first.equals(player.getUUID()) || player.getUUID().equals(e.getValue().second));
        player.removeEffect(ModEffects.ROOTED.get());
    }
    public static void updateBeam(ItemStack stack, ServerPlayer player) {
        if (!(stack.getItem() instanceof TeaWandItem wand) || !"ember_beam".equals(wand.wandId())) return;
        CompoundTag data = player.getPersistentData();
        int tick = data.getInt("BotaniaTeaBeamTick") + 1;
        data.putInt("BotaniaTeaBeamTick", tick);
        if (tick % 5 == 0 && !pay(stack, player, 20)) { player.stopUsingItem(); return; }
        Vec3 start=player.getEyePosition(), end=start.add(player.getViewVector(1).scale(24));
        var block=player.serverLevel().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if (block.getType()!=HitResult.Type.MISS) end=block.getLocation();
        if (tick%5==0) {
            Vec3 direction=end.subtract(start).normalize();
            for(double distance=0;distance<start.distanceTo(end);distance+=.5) {
                Vec3 point=start.add(direction.scale(distance));
                player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,point.x,point.y,point.z,1,.02,.02,.02,0);
            }
        }
        if (tick % 10 != 0) return;
        for (LivingEntity target:player.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(.2),e->canTarget(player,e))) {
            if (target.getBoundingBox().inflate(.2).clip(start,end).isPresent() && target.hurt(player.damageSources().onFire(),2)) target.setSecondsOnFire(3);
        }
    }

    public static void handleDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        LivingEntity protectedEntity = event.getEntity();
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float shield = protectedEntity.getPersistentData().getFloat(SHIELD);
        if (shield <= 0 || event.getAmount() <= 0) return;
        float absorbed = Math.min(shield, event.getAmount());
        protectedEntity.getPersistentData().putFloat(SHIELD, shield - absorbed);
        event.setAmount(event.getAmount() - absorbed);
    }

    public static void handleBranchDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (PROPAGATING.get() || event.getAmount() <= 0) return;
        LivingEntity victim = event.getEntity();
        UUID victimId = victim.getUUID();
        for (BranchLink link : LINKS.values()) {
            if (link.expiresAt < now(victim) || link.second == null || (!link.first.equals(victimId) && !link.second.equals(victimId))) continue;
            UUID otherId = link.first.equals(victimId) ? link.second : link.first;
            if (!(victim.level() instanceof ServerLevel level)) continue;
            LivingEntity other = find(level, otherId);
            if (other == null || other == victim || !other.isAlive() || other.distanceTo(victim)>24 || !level.dimension().location().toString().equals(link.dimension)) continue;
            try { PROPAGATING.set(true); other.hurt(event.getSource(), event.getAmount() * .5F); }
            finally { PROPAGATING.set(false); }
            break;
        }
    }

    public static void tick(long gameTime, Iterable<ServerLevel> levels) {
        Iterator<Map.Entry<UUID, BranchLink>> it = LINKS.entrySet().iterator();
        while (it.hasNext()) {
            var entry=it.next(); var link=entry.getValue(); ServerLevel linkedLevel=null;
            for(var level:levels) if(level.dimension().location().toString().equals(link.dimension)) linkedLevel=level;
            LivingEntity first=linkedLevel==null?null:find(linkedLevel,link.first);
            LivingEntity second=linkedLevel==null || link.second==null?null:find(linkedLevel,link.second);
            if (link.expiresAt<=gameTime || first==null || !first.isAlive() || (link.second!=null && (second==null || !second.isAlive() || first.distanceTo(second)>24))) it.remove();
        }
    }
    private static boolean canTarget(ServerPlayer player,LivingEntity target) {
        if (target==player || !target.isAlive() || target.isAlliedTo(player) || target.isSpectator()) return false;
        if (target instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isOwnedBy(player)) return false;
        return !(target instanceof net.minecraft.world.entity.player.Player other) || player.canHarmPlayer(other);
    }

    private static LivingEntity rayTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition(1.0F), end = start.add(player.getViewVector(1.0F).scale(range));
        BlockHitResult blockHit = player.serverLevel().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double blockLimit = blockHit.getType() == HitResult.Type.MISS ? range * range : start.distanceToSqr(blockHit.getLocation());
        AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(.5);
        EntityHitResult hit = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, search, e -> canTarget(player,e))) {
            var clip = entity.getBoundingBox().inflate(.2).clip(start, end);
            if (clip.isPresent() && start.distanceToSqr(clip.get()) <= blockLimit && start.distanceToSqr(clip.get()) < best) {
                best = start.distanceToSqr(clip.get()); hit = new EntityHitResult(entity, clip.get());
            }
        }
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }
    private static boolean safeDestination(ServerPlayer player, Vec3 destination) {
        if (!player.serverLevel().hasChunkAt(BlockPos.containing(destination)) || !player.serverLevel().getWorldBorder().isWithinBounds(BlockPos.containing(destination))) return false;
        AABB moved = player.getBoundingBox().move(destination.subtract(player.position()));
        if (destination.distanceToSqr(player.position())>30*30 || moved.minY<player.level().getMinBuildHeight() || moved.maxY>player.level().getMaxBuildHeight() || !player.serverLevel().getWorldBorder().isWithinBounds(moved)) return false;
        return player.serverLevel().noCollision(player, moved) && !player.serverLevel().containsAnyLiquid(moved);
    }
    private static boolean pay(ItemStack stack, ServerPlayer player, int amount) { return ManaItemHandler.instance().requestManaExactForTool(stack, player, amount, true); }
    private static long now(LivingEntity entity) { return entity.level().getServer() == null ? entity.level().getGameTime() : entity.level().getServer().overworld().getGameTime(); }
    private static boolean onCooldown(ServerPlayer player, String id) { return player.getPersistentData().getCompound(COOLDOWNS).getLong(id) > now(player); }
    private static void setCooldown(ServerPlayer player, String id, long duration) { CompoundTag tag = player.getPersistentData().getCompound(COOLDOWNS); tag.putLong(id, now(player) + duration); player.getPersistentData().put(COOLDOWNS, tag); }
    private static LivingEntity find(ServerLevel level, UUID uuid) { return level.getEntity(uuid) instanceof LivingEntity entity ? entity : null; }
    private record BranchLink(UUID first, UUID second, long expiresAt, String dimension) {}
}
