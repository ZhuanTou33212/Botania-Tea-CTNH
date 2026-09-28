package com.qianxin.botaniatea.content;

import com.qianxin.botaniatea.block.entity.ManaOfferingBlockEntity;
import com.qianxin.botaniatea.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;
import vazkii.botania.common.item.BotaniaItems;
import java.math.BigInteger;
import java.util.Comparator;

/** Server-owned assembly, binding and exact output-based mana pricing. */
public final class RitualAssembly {
    private RitualAssembly() {}
    /** Offering tables anywhere in the altar 7x7 ring, in a stable order so consumption never drifts. */
    public static java.util.List<ManaOfferingBlockEntity> tables(Level level, BlockPos center) {
        if (!level.hasChunkAt(center)) return java.util.List.of();
        var found = new java.util.ArrayList<ManaOfferingBlockEntity>();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-3,0,-3), center.offset(3,0,3))) {
            if (p.equals(center) || !level.hasChunkAt(p)) continue;
            if (level.getBlockEntity(p) instanceof ManaOfferingBlockEntity table) found.add(table);
        }
        found.sort(java.util.Comparator.<ManaOfferingBlockEntity>comparingInt(t -> t.getBlockPos().getZ())
                .thenComparingInt(t -> t.getBlockPos().getX()));
        return java.util.List.copyOf(found);
    }

    /** The altar near an offering table, or null when this table is not part of a circle. */
    public static BlockPos altarNear(Level level, BlockPos pos) {
        BlockPos best = null; double bestKey = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3,0,-3), pos.offset(3,0,3))) {
            if (p.equals(pos) || !level.hasChunkAt(p)) continue;
            if (!level.getBlockState(p).is(BotaniaBlocks.runeAltar)) continue;
            double key = p.distSqr(pos);
            if (key < bestKey || (key == bestKey && (best == null || p.asLong() < best.asLong()))) { bestKey = key; best = p.immutable(); }
        }
        return best;
    }

    /** The player builds the base: every cell of the 7x7 layer under the altar must be livingrock. */
    public static boolean foundationIntact(Level level, BlockPos center) {
        if (level.isClientSide || !level.hasChunkAt(center)) return false;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-3,-1,-3), center.offset(3,-1,3))) {
            if (!level.hasChunkAt(p) || level.isOutsideBuildHeight(p)) return false;
            if (!level.getBlockState(p).is(BotaniaBlocks.livingrock)) return false;
        }
        return true;
    }

    public static boolean complete(Level level, BlockPos center) {
        return level.hasChunkAt(center) && level.getBlockState(center).is(BotaniaBlocks.runeAltar)
                && !tables(level, center).isEmpty();
    }
    /**
     * Validates the player-built base. Nothing is written, erased or spawned here: an incomplete
     * or partially broken livingrock layer simply leaves the circle inactive.
     */
    public static boolean formFoundation(Level level, BlockPos center, ManaOfferingBlockEntity owner) {
        if (level.isClientSide) return false;
        if (!level.getBlockState(center).is(BotaniaBlocks.runeAltar)) return false;
        if (tables(level, center).isEmpty()) return false;
        boolean intact = foundationIntact(level, center);
        if (owner.foundationFormed != intact) { owner.foundationFormed = intact; owner.setChanged(); }
        return intact;
    }
    public static boolean inPoolRange(BlockPos center, BlockPos pool) {
        return Math.abs((long)center.getX()-pool.getX())<=2 && Math.abs((long)center.getZ()-pool.getZ())<=2
                && Math.abs((long)center.getY()-pool.getY())<=2;
    }
    private static ManaPool poolAt(Level level, BlockPos pos) {
        if (pos==null || !level.hasChunkAt(pos)) return null;
        var be=level.getBlockEntity(pos);
        if (be==null) return null;
        var receiver=be.getCapability(BotaniaForgeCapabilities.MANA_RECEIVER).resolve().orElse(null);
        if (receiver instanceof ManaPool pool) return pool;
        // Botania's own pool block entity is always a ManaPool, so a transient capability miss must
        // not read as "there is no pool here" and leave the ritual silently unpaid.
        return be instanceof ManaPool pool ? pool : null;
    }
    public static void bind(Level level, BlockPos center, ManaOfferingBlockEntity owner) {
        if (level.isClientSide) return;
        if (owner.boundPool!=null && inPoolRange(center,owner.boundPool) && poolAt(level,owner.boundPool)!=null) return;
        BlockPos selected=BlockPos.betweenClosedStream(center.offset(-2,-2,-2),center.offset(2,2,2))
                .map(BlockPos::immutable).filter(p -> poolAt(level,p)!=null)
                .min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(center)).thenComparingLong(BlockPos::asLong)).orElse(null);
        if (!java.util.Objects.equals(selected,owner.boundPool)) { owner.boundPool=selected; owner.setChanged(); }
    }
    /** -1 means unknown, fractional, overflowed or missing pricing recipe; never a free fallback. */
    public static int manaCost(Level level, ItemStack result) {
        var profile=ElementAnalyzer.profile(result);
        if (profile==null || profile.vector()==null || !profile.vector().isIntegral() || result.isEmpty()) return -1;
        var redstone=new ItemStack(Items.REDSTONE);
        var reference=level.getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.MANA_INFUSION_TYPE).stream()
                .filter(r -> r.matches(redstone) && r.getResultItem(level.registryAccess()).is(BotaniaItems.manaPowder)
                        && r.getRecipeCatalyst()==null && r.getManaToConsume()>0)
                .sorted(Comparator.comparing(r -> r.getId().toString())).findFirst().orElse(null);
        if (reference==null) return -1;
        BigInteger total=profile.vector().values().values().stream().map(RationalAmount::numerator)
                .reduce(BigInteger.ZERO,BigInteger::add);
        try { return total.multiply(BigInteger.valueOf(result.getCount()))
                .multiply(BigInteger.valueOf(reference.getManaToConsume())).intValueExact(); }
        catch (ArithmeticException ex) { return -1; }
    }
    public static boolean take(Level level, BlockPos center, ManaOfferingBlockEntity owner, int amount, boolean simulate) {
        if (level.isClientSide || amount<0 || owner.boundPool==null || !inPoolRange(center,owner.boundPool)) return false;
        var pool=poolAt(level,owner.boundPool);
        if (pool==null || pool.getCurrentMana()<amount) return false;
        if (!simulate && amount>0) {
            pool.receiveMana(-amount);
            // Botania's own particle type, broadcast by the server only after successful payment.
            if (level instanceof ServerLevel server) {
                var start=owner.boundPool.getCenter().add(0,.3,0);
                var end=center.getCenter().add(0,.6,0);
                var particle=vazkii.botania.client.fx.WispParticleData.wisp(.18F,.2F,.8F,1F);
                for (int i=0;i<=16;i++) {
                    var p=start.lerp(end,i/16.0);
                    server.sendParticles(particle,p.x,p.y,p.z,1,0,0,0,0);
                }
            }
        }
        return true;
    }
}
