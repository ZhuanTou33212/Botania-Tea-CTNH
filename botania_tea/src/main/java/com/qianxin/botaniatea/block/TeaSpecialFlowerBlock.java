package com.qianxin.botaniatea.block;

import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import vazkii.botania.api.block_entity.SpecialFlowerBlockEntity;
import vazkii.botania.forge.block.ForgeSpecialFlowerBlock;
import java.util.List;
import java.util.function.Supplier;

/** Same flower IDs and BE types; survival relocation preserves the existing state exactly once. */
public class TeaSpecialFlowerBlock extends ForgeSpecialFlowerBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("lit");
    public TeaSpecialFlowerBlock(MobEffect effect, int duration, Properties properties,
            Supplier<BlockEntityType<? extends SpecialFlowerBlockEntity>> type) {
        super(effect, duration, properties, type);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LIT);
    }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction side) {
        return level.getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower ? flower.extraWork().signal() : 0;
    }
    @Override public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (player.getItemInHand(hand).isEmpty()
                && level.getBlockEntity(pos) instanceof com.qianxin.botaniatea.block.entity.RegisteredGeneratingFlowerBlockEntity generator
                && com.qianxin.botaniatea.content.TeaFlower.fromBlock(state.getBlock())
                == com.qianxin.botaniatea.content.TeaFlower.SIX_HARMONY_CHRYSANTHEMUM) {
            if (!level.isClientSide) player.displayClientMessage(generator.harmonyStatus(), true);
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower) {
            ItemStack held = player.getItemInHand(hand);
            if (flower.flower() == com.qianxin.botaniatea.content.TeaFlower.BUILDING_BEGONIA) {
                return com.qianxin.botaniatea.server.BuildingSelectionService.useFlower(flower, player, hand);
            }
            if (flower.flower() == com.qianxin.botaniatea.content.TeaFlower.HOMING_MORNING_GLORY && held.isEmpty()) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    com.qianxin.botaniatea.server.HomingService.openSelection(serverPlayer, pos);
                }
                return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (flower.flower() == com.qianxin.botaniatea.content.TeaFlower.GAUGING_HYDRANGEA && !held.isEmpty()) {
                var fluid = net.minecraftforge.fluids.FluidUtil.getFluidContained(held.copy()).orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
                if (!fluid.isEmpty()) {
                    if (!level.isClientSide) {
                        flower.extraWork().selectMeasuredFluid(fluid.getFluid());
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal(flower.extraWork().summary()), true);
                    }
                    // This is a measurement filter, not an input tank; retain the calibration container.
                    return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            if (flower.flower() == com.qianxin.botaniatea.content.TeaFlower.CLEANSING_ANGELICA) {
                java.util.List<net.minecraft.world.effect.MobEffectInstance> effects;
                ItemStack remainder;
                if (held.isEmpty()) { effects = java.util.List.of(); remainder = ItemStack.EMPTY; }
                else if (held.getItem() instanceof com.qianxin.teaart.item.TeaDrinkItem tea) {
                    effects = tea.createEffects(); remainder = new ItemStack(com.qianxin.teaart.registry.ModItems.TEA_CUP.get());
                } else if (held.getItem() instanceof com.qianxin.botaniatea.item.AscendedTeaBottleItem tea) {
                    effects = tea.createEffects(); remainder = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                } else if (held.getItem() instanceof net.minecraft.world.item.PotionItem) {
                    effects = net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(held);
                    remainder = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                } else return super.use(state, level, pos, player, hand, hit);
                if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
                flower.configureEffects(effects);
                if (!held.isEmpty() && !player.getAbilities().instabuild) {
                    held.shrink(1);
                    if (held.isEmpty()) player.setItemInHand(hand, remainder);
                    else if (!player.addItem(remainder)) player.drop(remainder, false);
                }
                return net.minecraft.world.InteractionResult.CONSUME;
            }
            if (player.isShiftKeyDown() && held.isEmpty()) {
                if (!level.isClientSide) {
                    flower.extraWork().toggleMode();
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(flower.extraWork().summary()), true);
                }
                return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var drops = super.getDrops(state, builder);
        var be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be != null) {
            for (ItemStack drop : drops) {
                if (drop.is(asItem())) {
                    BlockItem.setBlockEntityData(drop, be.getType(), be.saveWithoutMetadata());
                    break;
                }
            }
        }
        return drops;
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower) {
            flower.releaseChunkLease();
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                com.qianxin.botaniatea.server.HomingService.remove(serverLevel, pos);
            }
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
