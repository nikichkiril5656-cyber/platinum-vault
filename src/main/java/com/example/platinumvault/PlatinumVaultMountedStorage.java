package com.example.platinumvault;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.codec.CreateCodecs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Содержимое платинового хранилища, пока оно едет на конструкции (корабле). */
public class PlatinumVaultMountedStorage extends WrapperMountedItemStorage<ItemStackHandler> {
	public static final MapCodec<PlatinumVaultMountedStorage> CODEC = CreateCodecs.ITEM_STACK_HANDLER.xmap(
		PlatinumVaultMountedStorage::new, storage -> storage.wrapped).fieldOf("value");

	protected PlatinumVaultMountedStorage(MountedItemStorageType<?> type, ItemStackHandler handler) {
		super(type, handler);
	}

	protected PlatinumVaultMountedStorage(ItemStackHandler handler) {
		this(ModRegistry.PLATINUM_VAULT_MOUNTED_STORAGE.get(), handler);
	}

	@Override
	public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
		if (be instanceof PlatinumVaultBlockEntity vault)
			vault.applyInventoryToBlock(wrapped);
	}

	@Override
	public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureBlockInfo info) {
		// как и у хранилища Create, открыть его на конструкции нельзя
		return false;
	}

	public static PlatinumVaultMountedStorage fromVault(PlatinumVaultBlockEntity vault) {
		// копируем в безопасный обработчик, чтобы не трогать мир
		return new PlatinumVaultMountedStorage(copyToItemStackHandler(vault.getInventoryOfBlock()));
	}
}
