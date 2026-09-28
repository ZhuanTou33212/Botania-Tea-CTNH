package com.qianxin.botaniatea.block;

import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import com.qianxin.botaniatea.block.entity.RegisteredGeneratingFlowerBlockEntity;
import com.qianxin.botaniatea.content.TeaFlower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import vazkii.botania.api.block_entity.SpecialFlowerBlockEntity;
import vazkii.botania.common.block.FloatingSpecialFlowerBlock;

import java.util.List;
import java.util.function.Supplier;

/** Tea Garden floating flower using Botania's native floating-flower renderer and BE state. */
public class TeaFloatingSpecialFlowerBlock extends FloatingSpecialFlowerBlock {
    public TeaFloatingSpecialFlowerBlock(Properties properties,
            Supplier<BlockEntityType<? extends SpecialFlowerBlockEntity>> type) {
        super(properties, type);
        registerDefaultState(defaultBlockState().setValue(TeaSpecialFlowerBlock.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TeaSpecialFlowerBlock.LIT);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.core.Direction side) {
        return level.getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower
                ? flower.extraWork().signal() : 0;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (level.getBlockEntity(pos) instanceof RegisteredGeneratingFlowerBlockEntity generator
                && held.isEmpty()
                && state.is(com.qianxin.botaniatea.registry.ModBlocks.FLOATING_FLOWERS
                .get(TeaFlower.SIX_HARMONY_CHRYSANTHEMUM).get())) {
            if (!level.isClientSide) player.displayClientMessage(generator.harmonyStatus(), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower) {
            if (flower.flower() == TeaFlower.BUILDING_BEGONIA) {
                return com.qianxin.botaniatea.server.BuildingSelectionService.useFlower(flower, player, hand);
            }
            if (flower.flower() == TeaFlower.HOMING_MORNING_GLORY && held.isEmpty()) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    com.qianxin.botaniatea.server.HomingService.openSelection(serverPlayer, pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (flower.flower() == TeaFlower.GAUGING_HYDRANGEA && !held.isEmpty()) {
                var fluid = net.minecraftforge.fluids.FluidUtil.getFluidContained(held.copy())
                        .orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
                if (!fluid.isEmpty()) {
                    if (!level.isClientSide) {
                        flower.extraWork().selectMeasuredFluid(fluid.getFluid());
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                flower.extraWork().summary()), true);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            if (flower.flower() == TeaFlower.CLEANSING_ANGELICA) {
                List<net.minecraft.world.effect.MobEffectInstance> effects;
                ItemStack remainder;
                if (held.isEmpty()) {
                    effects = List.of();
                    remainder = ItemStack.EMPTY;
                } else if (held.getItem() instanceof com.qianxin.teaart.item.TeaDrinkItem tea) {
                    effects = tea.createEffects();
                    remainder = new ItemStack(com.qianxin.teaart.registry.ModItems.TEA_CUP.get());
                } else if (held.getItem() instanceof com.qianxin.botaniatea.item.AscendedTeaBottleItem tea) {
                    effects = tea.createEffects();
                    remainder = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                } else if (held.getItem() instanceof net.minecraft.world.item.PotionItem) {
                    effects = net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(held);
                    remainder = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                } else {
                    return super.use(state, level, pos, player, hand, hit);
                }
                if (level.isClientSide) return InteractionResult.SUCCESS;
                flower.configureEffects(effects);
                if (!held.isEmpty() && !player.getAbilities().instabuild) {
                    held.shrink(1);
                    if (held.isEmpty()) player.setItemInHand(hand, remainder);
                    else if (!player.addItem(remainder)) player.drop(remainder, false);
                }
                return InteractionResult.CONSUME;
            }
            if (player.isShiftKeyDown() && held.isEmpty()) {
                if (!level.isClientSide) {
                    flower.extraWork().toggleMode();
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            flower.extraWork().summary()), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        var blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity != null) {
            for (ItemStack drop : drops) {
                if (drop.is(asItem())) {
                    BlockItem.setBlockEntityData(drop, blockEntity.getType(), blockEntity.saveWithoutMetadata());
                    break;
                }
            }
        }
        return drops;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower) {
            flower.releaseChunkLease();
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                com.qianxin.botaniatea.server.HomingService.remove(serverLevel, pos);
            }
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
