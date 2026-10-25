package com.example.platinumvault;

import com.simibubi.create.CreateClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import com.simibubi.create.foundation.block.connected.CTModel;

/** Только клиент: подключает соединяемые текстуры Create к нашему блоку. */
public class ClientSetup {
	public static void init(IEventBus modBus) {
		modBus.addListener((RegisterMenuScreensEvent e) ->
			e.register(ModRegistry.PLATINUM_HATCH_MENU.get(), PlatinumHatchScreen::new));
		PlatinumVaultCTBehaviour behaviour = new PlatinumVaultCTBehaviour();
		CreateClient.MODEL_SWAPPER.getCustomBlockModels()
			.register(ModRegistry.PLATINUM_VAULT.getId(), model -> new CTModel(model, behaviour));
	}
}
