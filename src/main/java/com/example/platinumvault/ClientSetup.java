package com.example.platinumvault;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.logistics.funnel.FunnelRenderer;
import com.simibubi.create.content.logistics.funnel.FunnelVisual;
import com.simibubi.create.foundation.block.connected.CTModel;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Только клиент: соединяемые текстуры хранилища, окно фильтра и отрисовка заслонки шлюза. */
public class ClientSetup {
	public static void init(IEventBus modBus) {
		PlatinumVaultCTBehaviour behaviour = new PlatinumVaultCTBehaviour();
		CreateClient.MODEL_SWAPPER.getCustomBlockModels()
			.register(ModRegistry.PLATINUM_VAULT.getId(), model -> new CTModel(model, behaviour));

		modBus.addListener((RegisterMenuScreensEvent e) ->
			e.register(ModRegistry.PLATINUM_FUNNEL_MENU.get(), PlatinumFunnelScreen::new));

		// отрисовка заслонки воронки: обычный рендер (без Flywheel)...
		modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
			e.registerBlockEntityRenderer(ModRegistry.PLATINUM_FUNNEL_BE.get(), FunnelRenderer::new));

		// ...и визуал Flywheel, как у воронки Create
		modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() ->
			SimpleBlockEntityVisualizer.builder(ModRegistry.PLATINUM_FUNNEL_BE.get())
				.factory(FunnelVisual::new)
				.skipVanillaRender(be -> false)
				.apply()));
	}
}
