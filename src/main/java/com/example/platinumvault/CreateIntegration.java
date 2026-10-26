package com.example.platinumvault;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.logistics.funnel.FunnelMovementBehaviour;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.packager.InventoryIdentifier;

import net.minecraft.world.level.block.entity.BlockEntity;

/** Подключение платинового хранилища к механикам Create: конструкции, упаковщики. */
public class CreateIntegration {

	public static void setup() {
		// 1. Хранилище на конструкциях: Create должен знать, как «упаковать» наш блок
		MountedItemStorageType.REGISTRY.register(ModRegistry.PLATINUM_VAULT.get(),
			ModRegistry.PLATINUM_VAULT_MOUNTED_STORAGE.get());

		// Шлюзы на конструкциях работают как латунные воронки (фильтр учитывается)
		FunnelMovementBehaviour funnelBehaviour = FunnelMovementBehaviour.brass();
		MovementBehaviour.REGISTRY.register(ModRegistry.PLATINUM_FUNNEL.get(), funnelBehaviour);

		// Склеенные в мультиблок хранилища двигаются вместе
		BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) -> {
			if (state.getBlock() instanceof PlatinumVaultBlock)
				return CheckResult.of(ConnectivityHandler.isConnected(level, pos, pos.relative(direction)));
			return CheckResult.PASS;
		});

		// 3. Упаковщики: одно хранилище = один инвентарь
		InventoryIdentifier.REGISTRY.register(ModRegistry.PLATINUM_VAULT.get(), (level, state, face) -> {
			BlockEntity be = level.getBlockEntity(face.getPos());
			return be instanceof PlatinumVaultBlockEntity vault ? vault.getInvId() : null;
		});
	}
}
