package com.example.platinumvault;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryWrapper;
import com.simibubi.create.foundation.mixin.accessor.ItemStackHandlerAccessor;
import com.simibubi.create.foundation.utility.SameSizeCombinedInvWrapper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

public class PlatinumVaultBlockEntity extends SmartBlockEntity
	implements IMultiBlockEntityContainer.Inventory, Clearable {

	/** Платиновое хранилище вмещает вдвое больше обычного (в слотах на один блок). */
	private static final int CAPACITY_MULTIPLIER = 2;

	protected ICapabilityProvider<IItemHandler> itemCapability = null;
	protected ItemStackHandler inventory;
	protected BlockPos controller;
	protected BlockPos lastKnownPos;
	protected boolean updateConnectivity;
	protected int radius = 1;
	protected int length = 1;

	public PlatinumVaultBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inventory = new ItemStackHandler(AllConfigs.server().logistics.vaultCapacity.get() * CAPACITY_MULTIPLIER) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				updateComparators();
				level.blockEntityChanged(worldPosition);
			}
		};
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Nullable
	public IItemHandler getItemCapability() {
		initCapability();
		return itemCapability == null ? null : itemCapability.getCapability();
	}

	protected void updateConnectivity() {
		updateConnectivity = false;
		if (level.isClientSide() || !isController())
			return;
		ConnectivityHandler.formMulti(this);
	}

	protected void updateComparators() {
		PlatinumVaultBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return;
		level.blockEntityChanged(controllerBE.worldPosition);

		Axis axis = controllerBE.getMainConnectionAxis();
		int zMax = axis == Axis.X ? controllerBE.radius : controllerBE.length;
		int xMax = axis == Axis.Z ? controllerBE.radius : controllerBE.length;
		BlockPos base = controllerBE.getBlockPos();
		for (int y = 0; y < controllerBE.radius; y++)
			for (int z = 0; z < zMax; z++)
				for (int x = 0; x < xMax; x++) {
					BlockPos p = base.offset(x, y, z);
					if (level.hasChunkAt(p))
						level.updateNeighbourForOutputSignal(p, getBlockState().getBlock());
				}
	}

	@Override
	public void tick() {
		super.tick();
		if (lastKnownPos == null)
			lastKnownPos = getBlockPos();
		else if (!lastKnownPos.equals(worldPosition) && worldPosition != null) {
			removeController(true);
			lastKnownPos = worldPosition;
			return;
		}
		if (updateConnectivity)
			updateConnectivity();
	}

	@Override
	public BlockPos getLastKnownPos() {
		return lastKnownPos;
	}

	@Override
	public boolean isController() {
		return controller == null || worldPosition.equals(controller);
	}

	@SuppressWarnings("unchecked")
	@Override
	public PlatinumVaultBlockEntity getControllerBE() {
		if (isController())
			return this;
		BlockEntity be = level.getBlockEntity(controller);
		return be instanceof PlatinumVaultBlockEntity v ? v : null;
	}

	public void removeController(boolean keepContents) {
		if (level.isClientSide())
			return;
		updateConnectivity = true;
		controller = null;
		radius = 1;
		length = 1;

		BlockState state = getBlockState();
		if (PlatinumVaultBlock.isVault(state)) {
			state = state.setValue(PlatinumVaultBlock.LARGE, false);
			getLevel().setBlock(worldPosition, state,
				Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
		}
		itemCapability = null;
		invalidateCapabilities();
		setChanged();
		sendData();
	}

	@Override
	public void setController(BlockPos controller) {
		if (level.isClientSide && !isVirtual())
			return;
		if (controller.equals(this.controller))
			return;
		this.controller = controller;
		itemCapability = null;
		invalidateCapabilities();
		setChanged();
		sendData();
	}

	@Override
	public BlockPos getController() {
		return isController() ? worldPosition : controller;
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);

		BlockPos controllerBefore = controller;
		int prevSize = radius;
		int prevLength = length;

		updateConnectivity = compound.contains("Uninitialized");
		lastKnownPos = compound.contains("LastKnownPos") ? NBTHelper.readBlockPos(compound, "LastKnownPos") : null;
		controller = compound.contains("Controller") ? NBTHelper.readBlockPos(compound, "Controller") : null;

		if (isController()) {
			radius = compound.getInt("Size");
			length = compound.getInt("Length");
		}

		if (!clientPacket) {
			inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
			return;
		}

		boolean changed = controllerBefore == null ? controller != null : !controllerBefore.equals(controller);
		if (hasLevel() && (changed || prevSize != radius || prevLength != length))
			level.setBlocksDirty(getBlockPos(), Blocks.AIR.defaultBlockState(), getBlockState());
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (updateConnectivity)
			compound.putBoolean("Uninitialized", true);
		if (lastKnownPos != null)
			compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
		if (!isController())
			compound.put("Controller", NbtUtils.writeBlockPos(controller));
		if (isController()) {
			compound.putInt("Size", radius);
			compound.putInt("Length", length);
		}
		super.write(compound, registries, clientPacket);
		if (!clientPacket)
			compound.put("Inventory", inventory.serializeNBT(registries));
	}

	@Override
	public void clearContent() {
		((ItemStackHandlerAccessor) inventory).create$getStacks().clear();
	}

	public ItemStackHandler getInventoryOfBlock() {
		return inventory;
	}

	private void initCapability() {
		if (itemCapability != null && itemCapability.getCapability() != null)
			return;
		if (!isController()) {
			PlatinumVaultBlockEntity controllerBE = getControllerBE();
			if (controllerBE == null)
				return;
			controllerBE.initCapability();
			itemCapability = ICapabilityProvider.of(() -> {
				if (controllerBE.isRemoved() || controllerBE.itemCapability == null)
					return null;
				return controllerBE.itemCapability.getCapability();
			});
			return;
		}

		boolean alongZ = PlatinumVaultBlock.getVaultBlockAxis(getBlockState()) == Axis.Z;
		IItemHandlerModifiable[] invs = new IItemHandlerModifiable[length * radius * radius];
		for (int y = 0; y < length; y++)
			for (int x = 0; x < radius; x++)
				for (int z = 0; z < radius; z++) {
					BlockPos p = alongZ ? worldPosition.offset(x, z, y) : worldPosition.offset(y, x, z);
					PlatinumVaultBlockEntity at =
						ConnectivityHandler.partAt(ModRegistry.PLATINUM_VAULT_BE.get(), level, p);
					invs[y * radius * radius + x * radius + z] =
						at != null ? at.inventory : new ItemStackHandler(inventory.getSlots());
				}
		itemCapability = ICapabilityProvider.of(new VersionedInventoryWrapper(SameSizeCombinedInvWrapper.create(invs)));
	}

	public static int getMaxLength(int radius) {
		return radius * 3;
	}

	@Override
	public void preventConnectivityUpdate() {
		updateConnectivity = false;
	}

	@Override
	public void notifyMultiUpdated() {
		BlockState state = getBlockState();
		if (PlatinumVaultBlock.isVault(state))
			level.setBlock(getBlockPos(), state.setValue(PlatinumVaultBlock.LARGE, radius > 2),
				Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
		itemCapability = null;
		invalidateCapabilities();
		setChanged();
	}

	@Override
	public Direction.Axis getMainConnectionAxis() {
		return getMainAxisOf(this);
	}

	@Override
	public int getMaxLength(Direction.Axis longAxis, int width) {
		return longAxis == Direction.Axis.Y ? getMaxWidth() : getMaxLength(width);
	}

	@Override
	public int getMaxWidth() {
		return 3;
	}

	@Override
	public int getHeight() {
		return length;
	}

	@Override
	public int getWidth() {
		return radius;
	}

	@Override
	public void setHeight(int height) {
		length = height;
	}

	@Override
	public void setWidth(int width) {
		radius = width;
	}

	@Override
	public boolean hasInventory() {
		return true;
	}
}
