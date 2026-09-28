package com.qianxin.botaniatea.content;

import com.qianxin.botaniatea.block.entity.ManaOfferingBlockEntity;
import com.qianxin.botaniatea.recipe.OfferingCraftingRecipe;
import com.qianxin.botaniatea.recipe.OfferingEnchantingRecipe;
import com.qianxin.botaniatea.registry.ModItems;
import com.qianxin.botaniatea.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.botania.common.block.block_entity.RunicAltarBlockEntity;
import java.util.*;

/**
 * North pedestal owns the ritual state. All coordinates are in the altar's horizontal plane.
 *
 * <p>This multiblock (a runic altar surrounded by five mana offering tables) is the single
 * crafting station: it runs both {@code offering_crafting} recipes and {@code offering_enchanting}
 * recipes. The crucible itself only distils essence and never crafts.
 */
public final class OfferingRitual {
    private OfferingRitual() {}
    public static List<BlockPos> positions(BlockPos center) {
        return List.of(center.offset(0,0,-3), center.offset(-2,0,-1), center.offset(2,0,-1),
                center.offset(-2,0,3), center.offset(2,0,3));
    }
    public static void tick(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, ManaOfferingBlockEntity self) {
        if (level.isClientSide) return;
        BlockPos center = RitualAssembly.altarNear(level, pos);
        if (center == null) { self.resetRitual(); return; }
        var tables = RitualAssembly.tables(level, center);
        // Exactly one table drives the ritual: the first in the stable order. The rest are inputs.
        if (tables.isEmpty() || !tables.get(0).getBlockPos().equals(pos)) return;
        ManaOfferingBlockEntity owner = tables.get(0);
        if (!(level.getBlockEntity(center) instanceof RunicAltarBlockEntity altar)) { owner.resetRitual(); return; }
        // The player builds the 7x7 livingrock base; breaking it clears the activation.
        if (!RitualAssembly.formFoundation(level, center, owner)) {
            if (owner.circleActive) { owner.circleActive=false; owner.setChanged(); }
            owner.resetRitual(); return;
        }
        // A wand of the forest must have lit the circle before anything happens.
        if (!owner.circleActive) { owner.resetRitual(); return; }
        RitualAssembly.bind(level, center, owner);
        if (!level.hasNeighborSignal(center)) { owner.resetRitual(); return; }
        // Faces define stable order; empty frames do not whitelist anything.
        var frames = level.getEntitiesOfClass(ItemFrame.class, new AABB(center).inflate(2),
                frame -> frame.getPos().relative(frame.getDirection().getOpposite()).equals(center));
        frames.sort(Comparator.comparingInt(frame -> frame.getDirection().ordinal()));
        var filters = frames.stream().map(ItemFrame::getItem).filter(stack -> !stack.isEmpty()).toList();
        var access = level.registryAccess();
        var craftingRecipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.OFFERING_CRAFTING.get()).stream()
                .sorted(Comparator.comparing(recipe -> recipe.getId().toString())).toList();
        var enchantingRecipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.OFFERING_ENCHANTING.get()).stream()
                .sorted(Comparator.comparing(recipe -> recipe.getId().toString())).toList();

        OfferingCraftingRecipe selected = null;
        OfferingEnchantingRecipe selectedEnchanting = null;
        ItemStack selectedUniversal = null;
        int selectedFilter = -1;
        int[] plan = null;
        for (int offset=0; offset < (frames.isEmpty()?1:filters.size()); offset++) {
            int filter = filters.isEmpty() ? -1 : Math.floorMod(owner.nextFilter + offset, filters.size());
            for (var recipe : craftingRecipes) {
                // Enchanting ignores the item-frame whitelist: a random result cannot be named by a frame.
                if (filter >= 0 && !recipe.enchantable() && !ItemStack.isSameItemSameTags(recipe.getResultItem(access), filters.get(filter))) continue;
                if (RitualAssembly.manaCost(level,recipe.getResultItem(access))<0) continue;
                int[] candidate = plan(recipe, tables, altar);
                if (candidate != null) { selected=recipe; plan=candidate; selectedFilter=filter; break; }
            }
            if (selected != null) break;
            // Enchanting keeps the offer on the altar and pulls its crystals from the tables, so
            // the frame whitelist matches the item currently sitting on the altar.
            ItemStack offered=altar.getItemHandler().getItem(0);
            boolean catalystGated=enchantingRecipes.stream().anyMatch(r->r.catalystCount()>0 && r.acceptsTarget(offered));
            for (var recipe : enchantingRecipes) {
                if (!recipe.acceptsTarget(offered)) continue;
                if (catalystGated && recipe.catalystCount()==0) continue;
                // Enchanting never consults the whitelist, and a catalyst recipe owns its target so a
                // missing catalyst fails the craft instead of falling through to the generic recipe.
                if (!recipe.acceptsTarget(altar.getItemHandler().getItem(0))) continue;
                int[] candidate = planEnchanting(recipe, tables, altar);
                if (candidate != null) { selectedEnchanting=recipe; plan=candidate; selectedFilter=filter; break; }
            }
            if (selectedEnchanting != null) break;
            // Universal route: the frame names the target, the altar holds that target category
            // substrate and the tables hold its element crystals. Registered recipes always win.
            if (selected == null && selectedEnchanting == null && filter >= 0) {
                ItemStack target = filters.get(filter);
                int[] candidate = universalPlan(level, target, tables, altar);
                if (candidate != null) { selectedUniversal = target.copy(); selectedUniversal.setCount(1); plan = candidate; selectedFilter = filter; }
            }
            if (selectedUniversal != null) break;
        }
        int invested = selected != null && selected.enchantable() ? sum(plan) : 0;
        // An offering larger than one craft may hold wrecks the ritual instead of strengthening it.
        if (invested > 0 && EnchantPool.exceedsCapacity(invested)) {
            detonate(level, center, tables, altar, owner); return;
        }
        if (selected == null && selectedEnchanting == null && selectedUniversal == null) { owner.resetRitual(); return; }
        int cost = selected != null && !selected.enchantable()
                ? RitualAssembly.manaCost(level, selected.getResultItem(access))
                : selectedEnchanting != null ? selectedEnchanting.mana()
                : selectedUniversal != null ? RitualAssembly.manaCost(level, selectedUniversal) : -1;
        // Enchantable crafts roll against a seed drawn when the craft starts, so the price checked up
        // front and the book handed over at the end always describe the same enchantments.
        String signature = selectedUniversal != null ? "u="+ForgeRegistries.ITEMS.getKey(selectedUniversal.getItem())+":"+cost : (selected != null && selected.enchantable() ? "n="+invested : "cost="+cost);
        String key = (selected != null ? selected.getId().toString() : selectedEnchanting != null ? selectedEnchanting.getId().toString() : "botania_tea:universal")+":"+signature;
        if (!key.equals(owner.ritualRecipe)) {
            owner.ritualRecipe=key; owner.ritualProgress=0; owner.ritualSeed=level.random.nextLong();
        }
        ItemStack enchantedBook = selected != null && selected.enchantable()
                ? selected.assembleEnchantedBook(net.minecraft.util.RandomSource.create(owner.ritualSeed), invested)
                : ItemStack.EMPTY;
        if (selected != null && selected.enchantable()) {
            if (enchantedBook.isEmpty()) { owner.resetRitual(); return; }
            cost = RitualAssembly.manaCost(level, enchantedBook);
        }
        if (cost < 0 || !RitualAssembly.take(level, center, owner, cost, true)) { owner.resetRitual(); return; }
        owner.ritualProgress++; owner.setChanged();
        int duration = selected != null ? selected.duration() : selectedEnchanting != null ? selectedEnchanting.duration() : UNIVERSAL_DURATION;
        if (owner.ritualProgress < duration) { ritualParticles(level, center, tables, owner.ritualProgress); return; }
        // Inventory is rechecked every tick. Mutations below run in one server-thread operation.
        if (selected != null) {
            int[] finalPlan=plan(selected,tables,altar);
            if (finalPlan==null) { owner.resetRitual(); return; }
            plan=finalPlan;
            // Re-roll from the stored seed so the finished book matches the price already checked.
            var result = selected.enchantable()
                    ? selected.assembleEnchantedBook(net.minecraft.util.RandomSource.create(owner.ritualSeed), sum(finalPlan))
                    : selected.getResultItem(access);
            if (result.isEmpty()) { owner.resetRitual(); return; }
            if (!RitualAssembly.take(level, center, owner, cost, false)) { owner.resetRitual(); return; }
            ItemStack substrate=altar.getItemHandler().removeItem(0,selected.substrateCount());
            if (substrate.getCount()!=selected.substrateCount()) { owner.resetRitual(); return; }
            ItemStack spare=drainAltar(level, center, altar);
            consumeTables(level, center, tables, plan);
            popOut(level, center, result);
            if (!spare.isEmpty()) popOut(level, center, spare);
        } else if (selectedEnchanting != null) {
            int[] finalPlan=planEnchanting(selectedEnchanting,tables,altar);
            if (finalPlan==null) { owner.resetRitual(); return; }
            plan=finalPlan;
            ItemStack offer=altar.getItemHandler().getItem(0).copy();
            var result = selectedEnchanting.assembleFor(offer, level.random);
            if (result.isEmpty()) { owner.resetRitual(); return; }
            if (!RitualAssembly.take(level, center, owner, cost, false)) { owner.resetRitual(); return; }
            // The enchantment replaces the offer: the offer is consumed and only the result is handed over.
            drainAltar(level, center, altar);
            consumeTables(level, center, tables, plan);
            popOut(level, center, result);
        } else {
            // Universal craft: pay, take the substrate off the altar and hand over the named target.
            int[] finalPlan=universalPlan(level, selectedUniversal, tables, altar);
            if (finalPlan==null) { owner.resetRitual(); return; }
            plan=finalPlan;
            if (!RitualAssembly.take(level, center, owner, cost, false)) { owner.resetRitual(); return; }
            ItemStack substrate=altar.getItemHandler().removeItem(0,1);
            if (substrate.isEmpty()) { owner.resetRitual(); return; }
            ItemStack spare=drainAltar(level, center, altar);
            consumeTables(level, center, tables, plan);
            popOut(level, center, selectedUniversal);
            if (!spare.isEmpty()) popOut(level, center, spare);
        }
        craftBurst(level, center);
        if (selectedFilter >= 0) owner.nextFilter=(selectedFilter+1)%filters.size();
        owner.resetRitual();
    }

    /**
     * Takes everything off the altar and tells the watching clients about it. Botania syncs its altar
     * through its own packet, so a slot changed behind its back would stay rendered as a ghost item
     * that the player can still appear to take. The caller decides what happens to the removed stack:
     * the recipe already took its own substrate count, so anything found here is a bonus stack.
     */
    private static ItemStack drainAltar(Level level, BlockPos center, RunicAltarBlockEntity altar) {
        ItemStack removed=altar.getItemHandler().removeItem(0,64);
        level.sendBlockUpdated(center, level.getBlockState(center), level.getBlockState(center), 3);
        return removed;
    }

    /**
     * Ejects the finished stack above the altar exactly the way Botania's own rituals do: spawned at
     * y+1.5 and flagged as altar-spawned, because the altar's idle pickup otherwise swallows the
     * payout right back into the slot it came from.
     */
    private static void popOut(Level level, BlockPos center, ItemStack result) {
        if (result.isEmpty()) return;
        ItemEntity entity=new ItemEntity(level, center.getX()+.5, center.getY()+1.5, center.getZ()+.5, result.copy());
        vazkii.botania.xplat.XplatAbstractions.INSTANCE.itemFlagsComponent(entity).runicAltarSpawned=true;
        entity.setDeltaMovement(0.0, 0.12, 0.0);
        level.addFreshEntity(entity);
    }

    /** Consumes the planned amounts from each offering table, returning crafting remainders. */
    /** Universal crafts run a fixed duration: they have no recipe to declare one. */
    static final int UNIVERSAL_DURATION = 200;

    /**
     * Universal route: a frame names the target, the altar holds that target category substrate and
     * the tables hold its element crystals. Fractional or unresolved vectors are refused so nothing
     * can be conjured without a price, and one table per element type is required.
     */
    static int[] universalPlan(Level level, ItemStack target, List<ManaOfferingBlockEntity> tables, RunicAltarBlockEntity altar) {
        if (target.isEmpty()) return null;
        ItemStack base=altar.getItemHandler().getItem(0);
        if (!SubstrateCategory.accepts(target, base)) return null;
        var profile=ElementAnalyzer.profile(target);
        if (profile==null || profile.vector()==null || profile.vector().isEmpty() || !profile.vector().isIntegral()) return null;
        var required=new LinkedHashMap<net.minecraft.world.item.Item,Integer>();
        for (var entry : profile.vector().values().entrySet()) {
            if (entry.getValue().isZero()) continue;
            long amount=entry.getValue().numerator().longValueExact();
            var crystal=ModItems.ELEMENT_CRYSTALS.get(entry.getKey());
            if (amount<=0 || crystal==null || amount>Integer.MAX_VALUE) return null;
            required.merge(crystal.get(), (int) amount, Integer::sum);
        }
        if (required.isEmpty() || required.size()>tables.size()) return null;
        return allocate(required, tables, new int[tables.size()]);
    }

    private static void consumeTables(Level level, BlockPos center, List<ManaOfferingBlockEntity> tables, int[] plan) {
        for (int i=0;i<tables.size();i++) if (plan[i]>0) {
            ItemStack input = tables.get(i).inventory.extractItem(0, plan[i], false);
            if (input.hasCraftingRemainingItem()) {
                ItemStack returned = input.getCraftingRemainingItem(); returned.setCount(returned.getCount()*input.getCount());
                ItemStack extra = tables.get(i).inventory.insertItem(0, returned, false);
                if (!extra.isEmpty()) level.addFreshEntity(new ItemEntity(level, center.getX()+.5, center.getY()+1, center.getZ()+.5, extra));
            }
        }
    }

    static int[] plan(OfferingCraftingRecipe recipe, List<ManaOfferingBlockEntity> tables, RunicAltarBlockEntity altar) {
        ItemStack base=altar.getItemHandler().getItem(0);
        if (!recipe.getIngredients().get(0).test(base) || base.getCount()<recipe.substrateCount()) return null;
        int[] used=new int[tables.size()];
        var required=new LinkedHashMap<>(recipe.elements());
        if (recipe.enchantable()) {
            // The offering itself is the crystal count: take every magic crystal, and refuse to run
            // while anything else shares the tables.
            int offered=offeredMagicCrystals(tables,null);
            if (offered<=0) return null;
            required.put(magicCrystal(), offered);
        } else if (recipe.ascendedCount()>0) {
            required.merge(ModItems.ASCENDED_CRYSTAL.get(),recipe.ascendedCount(),Integer::sum);
        }
        return allocate(required, tables, used);
    }

    /** Total magic crystals offered on the tables, or -1 when a table holds a different item. */
    private static int offeredMagicCrystals(List<ManaOfferingBlockEntity> tables) {
        var magic=magicCrystal();
        if (magic==null) return -1;
        int total=0;
        for (var table : tables) {
            ItemStack stack=table.inventory.getStackInSlot(0);
            if (stack.isEmpty()) continue;
            if (!stack.is(magic)) return -1;
            total+=stack.getCount();
        }
        return total;
    }

    private static net.minecraft.world.item.Item magicCrystal() {
        var magic=ModItems.ELEMENT_CRYSTALS.get(ElementType.MAGIC);
        return magic==null ? null : magic.get();
    }

    private static int sum(int[] amounts) {
        int total=0;
        for (int amount : amounts) total+=amount;
        return total;
    }

    /**
     * Fails an oversized offering: the altar and every table are emptied with an explosion effect.
     * No block is destroyed and no mana is charged.
     */
    private static void detonate(Level level, BlockPos center, List<ManaOfferingBlockEntity> tables,
                                RunicAltarBlockEntity altar, ManaOfferingBlockEntity owner) {
        for (var table : tables) table.inventory.setStackInSlot(0, ItemStack.EMPTY);
        altar.getItemHandler().setItem(0, ItemStack.EMPTY);
        level.sendBlockUpdated(center, level.getBlockState(center), level.getBlockState(center), 3);
        double x=center.getX()+.5, y=center.getY()+.5, z=center.getZ()+.5;
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, x, y, z, 24, 0.4, 0.4, 0.4, 0.02);
        }
        level.playSound(null, center, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        owner.resetRitual();
    }

    /** Motes while a ritual runs: particles rise from the tables that are actually there into the altar. */
    private static void ritualParticles(Level level, BlockPos center, List<ManaOfferingBlockEntity> tables, int progress) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server) || progress % 4 != 0) return;
        for (ManaOfferingBlockEntity entry : tables) {
            BlockPos table = entry.getBlockPos();
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    table.getX()+.5, table.getY()+1.1, table.getZ()+.5, 2, 0.15, 0.15, 0.15, 0.0);
        }
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                center.getX()+.5, center.getY()+1.05, center.getZ()+.5, 1, 0.05, 0.1, 0.05, 0.01);
    }

    /** One-shot burst when a craft actually commits, so the payout reads as an event. */
    private static void craftBurst(Level level, BlockPos center) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return;
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                center.getX()+.5, center.getY()+.95, center.getZ()+.5, 24, 0.35, 0.35, 0.35, 0.05);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                center.getX()+.5, center.getY()+.95, center.getZ()+.5, 12, 0.4, 0.3, 0.4, 0.0);
    }

    /** Enchanting pulls its crystal cost and ascended crystals from the tables; the offer stays on the altar. */
    static int[] planEnchanting(OfferingEnchantingRecipe recipe, List<ManaOfferingBlockEntity> tables, RunicAltarBlockEntity altar) {
        ItemStack offer=altar.getItemHandler().getItem(0);
        if (!recipe.acceptsTarget(offer)) return null;
        int[] used=new int[tables.size()];
        var extra=recipe.catalystCount()>0 && recipe.catalyst().getItems().length>0 ? recipe.catalyst().getItems()[0].getItem() : null;
        var required=new LinkedHashMap<net.minecraft.world.item.Item,Integer>();
        if (recipe.random()) {
            // The offering itself is the crystal count, exactly like the book recipe.
            int invested=offeredMagicCrystals(tables, extra);
            if (invested<=0) return null;
            required.put(magicCrystal(), invested);
        } else {
            required.putAll(recipe.elements());
        }
        if(recipe.ascended()>0) required.merge(ModItems.ASCENDED_CRYSTAL.get(),recipe.ascended(),Integer::sum);
        if (extra!=null) required.merge(extra, recipe.catalystCount(), Integer::sum);
        return allocate(required, tables, used);
    }

    /** Magic crystals offered on the tables, or -1 when a table holds anything but magic crystals or the catalyst. */
    private static int offeredMagicCrystals(List<ManaOfferingBlockEntity> tables, net.minecraft.world.item.Item allowedExtra) {
        var magic=magicCrystal();
        if (magic==null) return -1;
        int total=0;
        for (var table : tables) {
            ItemStack stack=table.inventory.getStackInSlot(0);
            if (stack.isEmpty()) continue;
            if (stack.is(magic)) { total+=stack.getCount(); continue; }
            // Ascended crystals are consumed separately by the same plan, so they may share a table.
            if (stack.is(ModItems.ASCENDED_CRYSTAL.get())) continue;
            if (allowedExtra!=null && stack.is(allowedExtra)) continue;
            return -1;
        }
        return total;
    }


    /** Fills {@code used} from the single-slot tables; returns null when any requirement is short. */
    private static int[] allocate(Map<net.minecraft.world.item.Item,Integer> required, List<ManaOfferingBlockEntity> tables, int[] used) {
        for(var entry:required.entrySet()) {
            int left=entry.getValue();
            for(int i=0;i<tables.size() && left>0;i++) {
                ItemStack stack=tables.get(i).inventory.getStackInSlot(0);
                if(!stack.is(entry.getKey())) continue;
                int count=Math.min(left,stack.getCount()-used[i]); used[i]+=count; left-=count;
            }
            if(left>0) return null;
        }
        return used;
    }
}
