package com.example.platinumvault;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PlatinumVaultMountedStorageType extends MountedItemStorageType<PlatinumVaultMountedStorage> {
	public PlatinumVaultMountedStorageType() {
		super(PlatinumVaultMountedStorage.CODEC);
	}

	@Override
	@Nullable
	public PlatinumVaultMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
		return be instanceof PlatinumVaultBlockEntity vault ? PlatinumVaultMountedStorage.fromVault(vault) : null;
	}
}
