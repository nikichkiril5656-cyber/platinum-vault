package com.example.platinumvault;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.block.connected.CTModel;

/** Только клиент: подключает соединяемые текстуры Create к нашему блоку. */
public class ClientSetup {
	public static void init() {
		PlatinumVaultCTBehaviour behaviour = new PlatinumVaultCTBehaviour();
		CreateClient.MODEL_SWAPPER.getCustomBlockModels()
			.register(ModRegistry.PLATINUM_VAULT.getId(), model -> new CTModel(model, behaviour));
	}
}
