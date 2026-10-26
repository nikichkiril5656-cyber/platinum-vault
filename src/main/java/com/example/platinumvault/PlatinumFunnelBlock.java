package com.example.platinumvault;

import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Платиновый шлюз (воронка): вся механика унаследована от воронки Create. */
public class PlatinumFunnelBlock extends FunnelBlock {

	public PlatinumFunnelBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockState getEquivalentBeltFunnel(BlockGetter world, BlockPos pos, BlockState state) {
		Direction facing = getFacing(state);
		return ModRegistry.PLATINUM_BELT_FUNNEL.get().defaultBlockState()
			.setValue(BeltFunnelBlock.HORIZONTAL_FACING, facing)
			.setValue(POWERED, state.getValue(POWERED));
	}

	@Override
	public BlockEntityType<? extends FunnelBlockEntity> getBlockEntityType() {
		return ModRegistry.PLATINUM_FUNNEL_BE.get();
	}
}
