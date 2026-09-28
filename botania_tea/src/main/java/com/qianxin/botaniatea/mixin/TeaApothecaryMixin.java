package com.qianxin.botaniatea.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.api.block.PetalApothecary;
import vazkii.botania.common.block.block_entity.PetalApothecaryBlockEntity;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;
import vazkii.botania.common.helper.EntityHelper;
import vazkii.botania.xplat.XplatAbstractions;
import java.util.ArrayList;
import java.util.List;

/** Botania 452 rejects filled buckets and discards crafting remainders. Adapt only our recipes. */
@Mixin(value=PetalApothecaryBlockEntity.class, remap=false)
public abstract class TeaApothecaryMixin {
    @Unique private List<ItemStack> tea$remainders=List.of();
    @Inject(method="collideEntityItem",at=@At("HEAD"),cancellable=true,remap=false)
    private void tea$before(ItemEntity entity, CallbackInfoReturnable<Boolean> cir) {
        tea$remainders=List.of();
        var altar=(PetalApothecaryBlockEntity)(Object)this;
        var level=altar.getLevel();
        if(level==null || level.isClientSide || !entity.isAlive() || entity.getItem().isEmpty()
                || altar.getFluid()!=PetalApothecary.State.WATER) return;
        var inv=altar.getItemHandler();
        var recipe=level.getRecipeManager().getRecipeFor(BotaniaRecipeTypes.PETAL_TYPE,inv,level);
        if(recipe.isPresent() && recipe.get().getId().getNamespace().equals("botania_tea")
                && recipe.get().getReagent().test(entity.getItem())) {
            tea$remainders=new ArrayList<>();
            for(int i=0;i<inv.getContainerSize();i++) {
                var stack=inv.getItem(i);
                if(stack.hasCraftingRemainingItem()) tea$remainders.add(stack.getCraftingRemainingItem());
            }
        }
        // Water must already be in the basin: a second water bucket is a lily recipe ingredient.
        if(entity.getItem().is(Items.WATER_BUCKET)
                && !XplatAbstractions.INSTANCE.itemFlagsComponent(entity).apothecarySpawned) {
            boolean present=false;
            for(int i=0;i<inv.getContainerSize();i++) if(inv.getItem(i).is(Items.WATER_BUCKET)) present=true;
            if(!present) for(int i=0;i<inv.getContainerSize();i++) if(inv.getItem(i).isEmpty()) {
                inv.setItem(i,entity.getItem().copyWithCount(1)); EntityHelper.shrinkItem(entity);
                altar.setChanged();
                vazkii.botania.api.internal.VanillaPacketDispatcher.dispatchTEToNearbyPlayers(altar);
                cir.setReturnValue(true); return;
            }
        }
    }
    @Inject(method="collideEntityItem",at=@At("RETURN"),remap=false)
    private void tea$after(ItemEntity entity, CallbackInfoReturnable<Boolean> cir) {
        var altar=(PetalApothecaryBlockEntity)(Object)this;
        var remainders=tea$remainders; tea$remainders=List.of();
        if(!cir.getReturnValue() || altar.getFluid()!=PetalApothecary.State.EMPTY || altar.getLevel()==null) return;
        for(var stack:remainders) {
            var pos=altar.getBlockPos();
            var drop=new ItemEntity(altar.getLevel(),pos.getX()+.5,pos.getY()+1.5,pos.getZ()+.5,stack);
            XplatAbstractions.INSTANCE.itemFlagsComponent(drop).apothecarySpawned=true;
            altar.getLevel().addFreshEntity(drop);
        }
    }
}
