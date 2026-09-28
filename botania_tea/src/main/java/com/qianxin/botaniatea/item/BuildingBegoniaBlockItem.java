package com.qianxin.botaniatea.item;

import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import com.qianxin.botaniatea.server.BuildingSelection;
import com.qianxin.botaniatea.server.BuildingSelectionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.List;

/** Register under the existing building_begonia item ID. */
public final class BuildingBegoniaBlockItem extends BlockItem {
    public static final String BINDING = "TeaBuildingSource";
    public BuildingBegoniaBlockItem(Block block, Properties properties) { super(block, properties); }

    public static CompoundTag binding(ItemStack stack) {
        var tag = stack.getTagElement(BINDING);
        if (tag != null) return tag;
        // Survival drops already preserve the flower's Source through TeaSpecialFlowerBlock.
        var beTag = BlockItem.getBlockEntityData(stack);
        return beTag == null ? new CompoundTag() : beTag.getCompound("Shanhai").getCompound("Config").getCompound("Source");
    }

    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var be = level.hasChunkAt(pos) ? level.getBlockEntity(pos) : null;
        if (!BuildingSelectionService.canInteract(player, pos) || be == null
                || !be.getCapability(ForgeCapabilities.ITEM_HANDLER, context.getClickedFace()).isPresent()) {
            player.displayClientMessage(Component.translatable("message.botania_tea.building.invalid_container"), true);
            return InteractionResult.FAIL;
        }
        var tag = new CompoundTag();
        tag.putString("Dimension", level.dimension().location().toString());
        tag.putLong("Pos", pos.asLong()); tag.putInt("Face", context.getClickedFace().ordinal());
        stack.addTagElement(BINDING, tag);
        player.displayClientMessage(Component.translatable("message.botania_tea.building.bound", pos.getX(), pos.getY(), pos.getZ()), true);
        return InteractionResult.CONSUME;
    }

    @Override public InteractionResult place(BlockPlaceContext context) {
        CompoundTag tag = binding(context.getItemInHand());
        var level = context.getLevel();
        BlockPos source = BlockPos.of(tag.getLong("Pos"));
        var player = context.getPlayer();
        var be = level.hasChunkAt(source) ? level.getBlockEntity(source) : null;
        boolean valid = tag.contains("Pos") && tag.getString("Dimension").equals(level.dimension().location().toString())
                && BuildingSelection.nearContainer(context.getClickedPos(), source)
                && level.getWorldBorder().isWithinBounds(source) && be != null
                && be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.from3DDataValue(tag.getInt("Face"))).isPresent();
        if (!valid || player == null || !BuildingSelectionService.canInteract(player, source)
                || !BuildingSelectionService.canInteract(player, context.getClickedPos())) {
            if (player != null && !level.isClientSide) player.displayClientMessage(Component.translatable("message.botania_tea.building.plant_near_source"), true);
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    @Override protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level,
            net.minecraft.world.entity.player.Player player, ItemStack stack, BlockState state) {
        boolean updated = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower) {
            CompoundTag tag = binding(stack);
            flower.extraWork().buildingSelection().stop(); // Replanting never resumes the old task.
            boolean bound = flower.extraWork().bind(BlockPos.of(tag.getLong("Pos")),
                    Direction.from3DDataValue(tag.getInt("Face")), tag.getString("Dimension"));
            flower.setChanged(); flower.sync();
            return bound || updated;
        }
        return updated;
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.botania_tea.building.binding"));
        var tag = binding(stack);
        if (tag.contains("Pos")) {
            BlockPos pos = BlockPos.of(tag.getLong("Pos"));
            tooltip.add(Component.translatable("message.botania_tea.building.bound", pos.getX(), pos.getY(), pos.getZ()));
        }
    }
}
