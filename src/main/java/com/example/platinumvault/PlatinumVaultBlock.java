package com.example.platinumvault;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

/** Платиновое хранилище предметов: копия поведения хранилища Create, но с удвоенной вместимостью. */
public class PlatinumVaultBlock extends Block implements IWrenchable, IBE<PlatinumVaultBlockEntity> {

	public static final Property<Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	public static final BooleanProperty LARGE = BooleanProperty.create("large");

	public PlatinumVaultBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(LARGE, false));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_AXIS, LARGE);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		if (ctx.getPlayer() == null || !ctx.getPlayer().isShiftKeyDown()) {
			BlockState placedOn = ctx.getLevel()
				.getBlockState(ctx.getClickedPos().relative(ctx.getClickedFace().getOpposite()));
			Axis preferred = getVaultBlockAxis(placedOn);
			if (preferred != null)
				return defaultBlockState().setValue(HORIZONTAL_AXIS, preferred);
		}
		return defaultBlockState().setValue(HORIZONTAL_AXIS, ctx.getHorizontalDirection().getAxis());
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (oldState.getBlock() == state.getBlock() || isMoving)
			return;
		withBlockEntityDo(level, pos, PlatinumVaultBlockEntity::updateConnectivity);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (context.getClickedFace().getAxis().isVertical()) {
			BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
			if (be instanceof PlatinumVaultBlockEntity vault) {
				ConnectivityHandler.splitMulti(vault);
				vault.removeController(true);
			}
			state = state.setValue(LARGE, false);
		}
		return IWrenchable.super.onWrenched(state, context);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
			BlockEntity be = level.getBlockEntity(pos);
			if (!(be instanceof PlatinumVaultBlockEntity vaultBE))
				return;
			ItemHelper.dropContents(level, pos, vaultBE.getInventoryOfBlock());
			level.removeBlockEntity(pos);
			ConnectivityHandler.splitMulti(vaultBE);
		}
	}

	public static boolean isVault(BlockState state) {
		return state.getBlock() instanceof PlatinumVaultBlock;
	}

	@Nullable
	public static Axis getVaultBlockAxis(BlockState state) {
		return isVault(state) ? state.getValue(HORIZONTAL_AXIS) : null;
	}

	public static boolean isLarge(BlockState state) {
		return isVault(state) && state.getValue(LARGE);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		Axis axis = state.getValue(HORIZONTAL_AXIS);
		return state.setValue(HORIZONTAL_AXIS,
			rot.rotate(Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE)).getAxis());
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return ItemHelper.calcRedstoneFromBlockEntity(this, level, pos);
	}

	@Override
	public BlockEntityType<? extends PlatinumVaultBlockEntity> getBlockEntityType() {
		return ModRegistry.PLATINUM_VAULT_BE.get();
	}

	@Override
	public Class<PlatinumVaultBlockEntity> getBlockEntityClass() {
		return PlatinumVaultBlockEntity.class;
	}
}
