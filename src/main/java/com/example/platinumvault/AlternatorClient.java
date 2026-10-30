package com.example.platinumvault;

import com.reggarf.mods.create_better_motors.content.alternator.blocks.AlternatorRenderer;
import com.reggarf.mods.create_better_motors.content.alternator.blocks.AlternatorVisual;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Только клиент: отрисовка вращающихся валов платинового генератора, как у латунного. */
public final class AlternatorClient {
	private AlternatorClient() {}

	public static void init(IEventBus modBus) {
		modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
			e.registerBlockEntityRenderer(AlternatorModule.PLATINUM_ALTERNATOR_BE.get(), AlternatorRenderer::new));

		modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() ->
			SimpleBlockEntityVisualizer.builder(AlternatorModule.PLATINUM_ALTERNATOR_BE.get())
				.factory(AlternatorVisual::new)
				.skipVanillaRender(be -> false)
				.apply()));
	}
}
