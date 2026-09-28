package com.qianxin.botaniatea.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaPool;
import java.util.ArrayList;

public final class NearbyPoolMana {
    private NearbyPoolMana() {}
    /** Atomic on the server thread; a failed availability check never debits any pool. */
    public static boolean take(Level level, BlockPos origin, int amount, boolean simulate) {
        if (level.isClientSide || amount < 0) return false;
        var pools = new ArrayList<ManaPool>();
        long total = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-6,-2,-6), origin.offset(6,2,6))) {
            if (!level.hasChunkAt(pos)) continue;
            var be = level.getBlockEntity(pos);
            if (be == null) continue;
            var receiver = be.getCapability(BotaniaForgeCapabilities.MANA_RECEIVER).resolve().orElse(null);
            if (receiver instanceof ManaPool pool && pool.getCurrentMana() > 0) {
                pools.add(pool); total += pool.getCurrentMana();
            }
        }
        if (total < amount) return false;
        if (!simulate) for (ManaPool pool : pools) {
            int debit = Math.min(amount, pool.getCurrentMana());
            pool.receiveMana(-debit); amount -= debit;
            if (amount == 0) break;
        }
        return true;
    }
}
