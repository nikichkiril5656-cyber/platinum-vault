package com.example.platinumvault;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Соединяемые текстуры платинового хранилища (клиентский класс). */
public class PlatinumVaultCTBehaviour extends ConnectedTextureBehaviour.Base {

	private static CTSpriteShiftEntry shift(String part, String sheet) {
		ResourceLocation base = ResourceLocation.fromNamespaceAndPath(PlatinumVaultMod.MOD_ID, "block/vault_" + part + "_small");
		ResourceLocation connected =
			ResourceLocation.fromNamespaceAndPath(PlatinumVaultMod.MOD_ID, "block/vault_" + part + "_" + sheet);
		return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, base, connected);
	}

	// medium-лист для хранилища до 2x2, large-лист для 3x3
	private static final CTSpriteShiftEntry TOP_M = shift("top", "medium"), TOP_L = shift("top", "large"),
		FRONT_M = shift("front", "medium"), FRONT_L = shift("front", "large"),
		SIDE_M = shift("side", "medium"), SIDE_L = shift("side", "large"),
		BOTTOM_M = shift("bottom", "medium"), BOTTOM_L = shift("bottom", "large");

	@Override
	public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
		Axis axis = PlatinumVaultBlock.getVaultBlockAxis(state);
		if (axis == null)
			return null;
		boolean small = !PlatinumVaultBlock.isLarge(state);

		if (direction.getAxis() == axis)
			return small ? FRONT_M : FRONT_L;
		if (direction == Direction.UP)
			return small ? TOP_M : TOP_L;
		if (direction == Direction.DOWN)
			return small ? BOTTOM_M : BOTTOM_L;
		return small ? SIDE_M : SIDE_L;
	}

	@Override
	protected Direction getUpDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
		Axis axis = PlatinumVaultBlock.getVaultBlockAxis(state);
		boolean alongX = axis == Axis.X;
		if (face.getAxis().isVertical() && alongX)
			return super.getUpDirection(reader, pos, state, face).getClockWise();
		if (face.getAxis() == axis || face.getAxis().isVertical())
			return super.getUpDirection(reader, pos, state, face);
		return Direction.fromAxisAndDirection(axis, alongX ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE);
	}

	@Override
	protected Direction getRightDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
		Axis axis = PlatinumVaultBlock.getVaultBlockAxis(state);
		if (face.getAxis().isVertical() && axis == Axis.X)
			return super.getRightDirection(reader, pos, state, face).getClockWise();
		if (face.getAxis() == axis || face.getAxis().isVertical())
			return super.getRightDirection(reader, pos, state, face);
		return Direction.fromAxisAndDirection(Axis.Y, face.getAxisDirection());
	}

	@Override
	public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
		BlockPos otherPos, Direction face) {
		return state == other && ConnectivityHandler.isConnected(reader, pos, otherPos);
	}
}
