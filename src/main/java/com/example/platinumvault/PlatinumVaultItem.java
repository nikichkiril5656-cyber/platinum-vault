package com.example.platinumvault;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PlatinumVaultItem extends BlockItem {

	public PlatinumVaultItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext ctx) {
		InteractionResult result = super.place(ctx);
		if (!result.consumesAction())
			return result;
		tryMultiPlace(ctx);
		return result;
	}

	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack,
		BlockState state) {
		MinecraftServer server = level.getServer();
		if (server == null)
			return false;
		CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (data != null) {
			CompoundTag nbt = data.copyTag();
			nbt.remove("Length");
			nbt.remove("Size");
			nbt.remove("Controller");
			nbt.remove("LastKnownPos");
			BlockEntity.addEntityType(nbt, ((IBE<?>) getBlock()).getBlockEntityType());
			stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(nbt));
		}
		return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
	}

	private void tryMultiPlace(BlockPlaceContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null || player.isShiftKeyDown())
			return;
		Direction face = ctx.getClickedFace();
		ItemStack stack = ctx.getItemInHand();
		Level world = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockPos placedOnPos = pos.relative(face.getOpposite());
		BlockState placedOnState = world.getBlockState(placedOnPos);

		if (!PlatinumVaultBlock.isVault(placedOnState) || SymmetryWandItem.presentInHotbar(player))
			return;
		PlatinumVaultBlockEntity partAt =
			ConnectivityHandler.partAt(ModRegistry.PLATINUM_VAULT_BE.get(), world, placedOnPos);
		if (partAt == null)
			return;
		PlatinumVaultBlockEntity controllerBE = partAt.getControllerBE();
		if (controllerBE == null)
			return;

		int width = controllerBE.getWidth();
		if (width == 1)
			return;

		Axis axis = PlatinumVaultBlock.getVaultBlockAxis(placedOnState);
		if (axis == null || face.getAxis() != axis)
			return;

		Direction facing = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
		BlockPos startPos = face == facing.getOpposite() ? controllerBE.getBlockPos().relative(facing.getOpposite())
			: controllerBE.getBlockPos().relative(facing, controllerBE.getHeight());

		if (VecHelper.getCoordinate(startPos, axis) != VecHelper.getCoordinate(pos, axis))
			return;

		int toPlace = 0;
		for (int a = 0; a < width; a++) {
			for (int b = 0; b < width; b++) {
				BlockPos offsetPos = axis == Axis.X ? startPos.offset(0, a, b) : startPos.offset(a, b, 0);
				BlockState state = world.getBlockState(offsetPos);
				if (PlatinumVaultBlock.isVault(state))
					continue;
				if (!state.canBeReplaced())
					return;
				toPlace++;
			}
		}

		if (!player.isCreative() && stack.getCount() < toPlace)
			return;

		for (int a = 0; a < width; a++) {
			for (int b = 0; b < width; b++) {
				BlockPos offsetPos = axis == Axis.X ? startPos.offset(0, a, b) : startPos.offset(a, b, 0);
				if (PlatinumVaultBlock.isVault(world.getBlockState(offsetPos)))
					continue;
				super.place(BlockPlaceContext.at(ctx, offsetPos, face));
			}
		}
	}
}
