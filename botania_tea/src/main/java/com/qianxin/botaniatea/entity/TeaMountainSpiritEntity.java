package com.qianxin.botaniatea.entity;

import com.qianxin.botaniatea.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/** Server-authoritative ritual boss; normally defeated phantoms share the normal reward path. */
public final class TeaMountainSpiritEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> PHANTOM =
            SynchedEntityData.defineId(TeaMountainSpiritEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> LINKED_PLAYER =
            SynchedEntityData.defineId(TeaMountainSpiritEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(TeaMountainSpiritEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WINDUP = SynchedEntityData.defineId(TeaMountainSpiritEntity.class, EntityDataSerializers.INT);
    private static final float PHASE_TWO = 0.70F;
    private static final float PHASE_THREE = 0.35F;
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.translatable("entity.botania_tea.tea_mountain_spirit"),
            ServerBossEvent.BossBarColor.PURPLE, ServerBossEvent.BossBarOverlay.PROGRESS);
    private int phase = 1;
    private int phaseAttackDelay;
    private boolean dropsHandled;
    private boolean suppressLoot;
    private int missingPlayerTicks;
    private int targetSearchCooldown;
    private UUID eventId;
    private UUID summoner;
    private int attackCooldown, attackPhase = 1;
    private net.minecraft.world.phys.Vec3 attackOrigin = net.minecraft.world.phys.Vec3.ZERO;
    private net.minecraft.world.phys.Vec3 attackDirection = new net.minecraft.world.phys.Vec3(0,0,1);
    public int combatPhase() { return entityData.get(PHASE); }
    public int attackWindup() { return entityData.get(WINDUP); }

    public void setEventId(UUID id) { eventId = id; }
    public void setSummoner(UUID player) { summoner = player; }

    public TeaMountainSpiritEntity(EntityType<? extends TeaMountainSpiritEntity> type, Level level) {
        super(type, level);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(com.qianxin.botaniatea.config.TeaGardenConfig.BOSS_HEALTH.get());
        getAttribute(Attributes.ARMOR).setBaseValue(com.qianxin.botaniatea.config.TeaGardenConfig.BOSS_ARMOR.get());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(com.qianxin.botaniatea.config.TeaGardenConfig.BOSS_DAMAGE.get());
        setHealth(getMaxHealth());
        xpReward = 40;
    }

    public TeaMountainSpiritEntity(PlayMessages.SpawnEntity packet, Level level) {
        this(com.qianxin.botaniatea.registry.ModEntities.TEA_MOUNTAIN_SPIRIT.get(), level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 600.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                entity -> entity instanceof Player player && canTargetPlayer(player)));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 20.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    private boolean canTargetPlayer(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative()
                && (!isPhantom() || entityData.get(LINKED_PLAYER).filter(player.getUUID()::equals).isPresent());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return phaseAttackDelay == 0 && super.doHurtTarget(target);
    }

    public void setPhantom(@Nullable UUID linkedPlayer) {
        entityData.set(PHANTOM, true);
        entityData.set(LINKED_PLAYER, Optional.ofNullable(linkedPlayer));
        bossBar.setName(Component.translatable("entity.botania_tea.tea_mountain_spirit.phantom")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    public boolean isPhantom() {
        return entityData.get(PHANTOM);
    }

    @Override
    public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel)) return;
        bossBar.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
        if (isPhantom()) {
            Player linked = entityData.get(LINKED_PLAYER).map(serverLevel::getPlayerByUUID).orElse(null);
            if (linked != null && linked.isDeadOrDying()) {
                suppressLoot = true;
                discard();
                return;
            }
            if (linked == null) {
                setTarget(null);
                getNavigation().stop();
                if (++missingPlayerTicks >= 600) { suppressLoot = true; discard(); }
                return;
            }
            missingPlayerTicks = 0;
            if (canTargetPlayer(linked) && getTarget() != linked) setTarget(linked);
        }
        LivingEntity current = getTarget();
        if (current != null && (!(current instanceof Player player) || !canTargetPlayer(player))) setTarget(null);
        if (getTarget() == null) {
            if (--targetSearchCooldown <= 0) {
                targetSearchCooldown = 10;
                serverLevel.getEntitiesOfClass(Player.class, getBoundingBox().inflate(40, 12, 40),
                                player -> canTargetPlayer(player) && distanceToSqr(player) <= 40 * 40)
                        .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).ifPresent(this::setTarget);
            }
        } else targetSearchCooldown = 0;
        phase = getHealth() / getMaxHealth() <= PHASE_THREE ? 3 : getHealth() / getMaxHealth() <= PHASE_TWO ? 2 : 1;
        entityData.set(PHASE, phase);
        if (phaseAttackDelay > 0) {
            getNavigation().stop();
            if (phaseAttackDelay % 5 == 0) telegraph(serverLevel);
            if (--phaseAttackDelay == 0) {
                phaseAttack(serverLevel);
                attackCooldown = attackPhase == 1 ? 45 : attackPhase == 2 ? 55 : 65;
            }
        } else if (attackCooldown > 0) attackCooldown--;
        else if (getTarget() instanceof Player target && canTargetPlayer(target)
                && distanceToSqr(target) <= (phase == 1 ? 7 * 7 : phase == 2 ? 22 * 22 : 8 * 8)) {
            attackPhase = phase;
            attackOrigin = position();
            var direction = target.position().subtract(attackOrigin).multiply(1,0,1);
            attackDirection = direction.lengthSqr() < .001 ? new net.minecraft.world.phys.Vec3(0,0,1) : direction.normalize();
            phaseAttackDelay = phase == 1 ? 20 : phase == 2 ? 25 : 30;
            playSound(com.qianxin.botaniatea.registry.ModSounds.BOSS_WINDUP.get(), .8F, .7F + phase * .15F);
            telegraph(serverLevel);
        }
        entityData.set(WINDUP, phaseAttackDelay);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && phase < 3) {
            float ratio = getHealth() / getMaxHealth();
            int next = ratio <= PHASE_THREE ? 3 : ratio <= PHASE_TWO ? 2 : 1;
            if (next > phase) {
                phase = next;
                level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(24), p -> !p.isSpectator())
                        .forEach(p -> p.displayClientMessage(Component.translatable(
                                "message.botania_tea.mountain_spirit.phase", phase).withStyle(ChatFormatting.LIGHT_PURPLE), true));
            }
        }
        return hurt;
    }

    private void phaseAttack(ServerLevel level) {
        Vec3 origin = position();
        if (attackPhase == 2) {
            Vec3 launch = origin.add(0, 1.6, 0);
            Vec3 aim = getTarget() instanceof Player target && canTargetPlayer(target)
                    ? target.getEyePosition().subtract(launch).normalize() : attackDirection;
            for (int n = -2; n <= 2; n++) {
                Vec3 direction = aim.yRot((float) Math.toRadians(n * 10));
                var blade = com.qianxin.botaniatea.registry.ModEntities.TEA_BLADE.get().create(level);
                if (blade == null) continue;
                blade.setOwner(this);
                blade.setPos(launch.add(direction.scale(0.8)));
                blade.setDamage((float)getAttributeValue(Attributes.ATTACK_DAMAGE));
                blade.shoot(direction.x, direction.y, direction.z, 0.85F, 0);
                level.addFreshEntity(blade);
            }
            return;
        }
        for (Player target : level.getEntitiesOfClass(Player.class, new AABB(origin, origin).inflate(9, 3, 9),
                this::canTargetPlayer)) {
            Vec3 offset = target.position().subtract(origin);
            double along = offset.dot(attackDirection);
            double side = Math.abs(offset.x * attackDirection.z - offset.z * attackDirection.x);
            boolean hit = attackPhase == 1
                    ? along >= -0.5 && along <= 6 && side <= 1.25 + along * 0.35
                    : offset.horizontalDistanceSqr() <= 8 * 8;
            if (hit && Math.abs(offset.y) <= 2.5
                    && target.hurt(damageSources().mobAttack(this), (float)getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                target.knockback(0.5, origin.x - target.getX(), origin.z - target.getZ());
            }
        }
    }

    private void telegraph(ServerLevel level) {
        var dust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1F,.55F,.15F),1F);
        if (attackPhase == 3) {
            for(int step=0;step<32;step++) {
                double angle=step*Math.PI/16;
                level.sendParticles(dust,attackOrigin.x+Math.cos(angle)*8,
                        attackOrigin.y+.15,attackOrigin.z+Math.sin(angle)*8,1,0,0,0,0);
            }
            for (int step = -6; step <= 6; step += 2) {
                level.sendParticles(dust, attackOrigin.x + step, attackOrigin.y + .15, attackOrigin.z, 1, 0, 0, 0, 0);
                level.sendParticles(dust, attackOrigin.x, attackOrigin.y + .15, attackOrigin.z + step, 1, 0, 0, 0, 0);
            }
        } else {
            int spread = attackPhase == 1 ? 25 : 20;
            int range = attackPhase == 1 ? 6 : 18;
            for(int n=-1;n<=1;n++) {
                var direction=attackDirection.yRot((float)Math.toRadians(n*spread));
                for(int step=1;step<=range;step+=2) {
                    var point=attackOrigin.add(direction.scale(step));
                    level.sendParticles(dust,point.x,point.y+.15,point.z,1,0,0,0,0);
                }
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !dropsHandled && !suppressLoot) {
            dropsHandled = true;
            playSound(com.qianxin.botaniatea.registry.ModSounds.BOSS_DEFEAT.get(), 1F, .8F);
            Player winner = summoner == null ? null : ((ServerLevel) level()).getPlayerByUUID(summoner);
            if (winner == null && source.getEntity() instanceof Player player) winner = player;
            if (winner == null && isPhantom() && entityData.get(LINKED_PLAYER).isPresent())
                winner = ((ServerLevel) level()).getPlayerByUUID(entityData.get(LINKED_PLAYER).get());
            dropLoot((ServerLevel) level(), winner);
        }
        super.die(source);
    }

    private void dropLoot(ServerLevel level, @Nullable Player winner) {
        spawnAtLocation(ModItems.MOUNTAIN_HEARTWOOD.get(), 1);
        spawnAtLocation(ModItems.CLARITY_PORCELAIN_SHARD.get(), 2 + level.random.nextInt(3));
        spawnAtLocation(ModItems.TEA_MOUNTAIN_MEMORY.get(), 1);
        if (winner != null) {
            var persistent = winner.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            if (!persistent.getBoolean("TeaMountainFirstVictory")) {
                persistent.putBoolean("TeaMountainFirstVictory", true);
                winner.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistent);
                spawnAtLocation(ModItems.ANCIENT_TEA_SEED.get(), 1);
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(PHANTOM, false);
        entityData.define(LINKED_PLAYER, Optional.empty());
        entityData.define(PHASE,1); entityData.define(WINDUP,0);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(PHANTOM, tag.getBoolean("Phantom"));
        entityData.set(LINKED_PLAYER, tag.hasUUID("LinkedPlayer")
                ? Optional.of(tag.getUUID("LinkedPlayer")) : Optional.empty());
        phase = Math.max(1, Math.min(3, tag.getInt("Phase")));
        phaseAttackDelay = Math.max(0, tag.getInt("PhaseAttackDelay"));
        dropsHandled = tag.getBoolean("DropsHandled");
        suppressLoot = tag.getBoolean("SuppressLoot");
        missingPlayerTicks = Math.max(0, tag.getInt("MissingPlayerTicks"));
        eventId = tag.hasUUID("EventUuid") ? tag.getUUID("EventUuid") : null;
        summoner = tag.hasUUID("Summoner") ? tag.getUUID("Summoner") : null;
        attackCooldown = Math.max(0,tag.getInt("AttackCooldown"));
        attackPhase = Math.max(1,Math.min(3,tag.getInt("AttackPhase")));
        attackOrigin = new net.minecraft.world.phys.Vec3(tag.getDouble("AttackX"),tag.getDouble("AttackY"),tag.getDouble("AttackZ"));
        Vec3 savedDirection = new Vec3(tag.getDouble("AttackDX"), 0, tag.getDouble("AttackDZ"));
        attackDirection = savedDirection.lengthSqr() < .001 ? new Vec3(0, 0, 1) : savedDirection.normalize();
        entityData.set(PHASE,phase); entityData.set(WINDUP,phaseAttackDelay);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Phantom", isPhantom());
        entityData.get(LINKED_PLAYER).ifPresent(id -> tag.putUUID("LinkedPlayer", id));
        tag.putInt("Phase", phase);
        tag.putInt("PhaseAttackDelay", phaseAttackDelay);
        tag.putBoolean("DropsHandled", dropsHandled);
        tag.putBoolean("SuppressLoot", suppressLoot);
        tag.putInt("MissingPlayerTicks", missingPlayerTicks);
        if (eventId != null) tag.putUUID("EventUuid", eventId);
        if (summoner != null) tag.putUUID("Summoner", summoner);
        tag.putInt("AttackCooldown",attackCooldown); tag.putInt("AttackPhase",attackPhase);
        tag.putDouble("AttackX",attackOrigin.x);tag.putDouble("AttackY",attackOrigin.y);tag.putDouble("AttackZ",attackOrigin.z);
        tag.putDouble("AttackDX",attackDirection.x);tag.putDouble("AttackDZ",attackDirection.z);
    }

}
