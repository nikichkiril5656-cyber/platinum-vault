package com.example.platinumvault;

import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/** Платиновая воронка на механической ленте. Родительский блок задаётся через переопределения. */
public class PlatinumBeltFunnelBlock extends BeltFunnelBlock {

	public PlatinumBeltFunnelBlock(Properties properties) {
		super(null, properties);
	}

	@Override
	public boolean isOfSameType(FunnelBlock otherFunnel) {
		return otherFunnel == ModRegistry.PLATINUM_FUNNEL.get();
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
		Player player) {
		return new ItemStack(ModRegistry.PLATINUM_FUNNEL_ITEM.get());
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		return new ItemRequirement(ItemUseType.CONSUME, ModRegistry.PLATINUM_FUNNEL_ITEM.get());
	}

	@Override
	public BlockEntityType<? extends FunnelBlockEntity> getBlockEntityType() {
		return ModRegistry.PLATINUM_FUNNEL_BE.get();
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor world,
		BlockPos pos, BlockPos neighbourPos) {
		updateWater(world, state, pos);
		if (!isOnValidBelt(state, world, pos)) {
			BlockState parentState =
				ProperWaterloggedBlock.withWater(world, ModRegistry.PLATINUM_FUNNEL.get().defaultBlockState(), pos);
			if (state.getOptionalValue(POWERED).orElse(false))
				parentState = parentState.setValue(POWERED, true);
			if (state.getValue(SHAPE) == Shape.PUSHING)
				parentState = parentState.setValue(FunnelBlock.EXTRACTING, true);
			return parentState.setValue(FunnelBlock.FACING, state.getValue(HORIZONTAL_FACING));
		}
		Shape updatedShape =
			getShapeForPosition(world, pos, state.getValue(HORIZONTAL_FACING), state.getValue(SHAPE) == Shape.PUSHING);
		Shape currentShape = state.getValue(SHAPE);
		if (updatedShape == currentShape)
			return state;

		// не откатываем состояния, выставленные ключом
		if (updatedShape == Shape.PUSHING && currentShape == Shape.PULLING)
			return state;
		if (updatedShape == Shape.RETRACTED && currentShape == Shape.EXTENDED)
			return state;

		return state.setValue(SHAPE, updatedShape);
	}
}
