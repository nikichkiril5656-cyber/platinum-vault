package com.example.platinumvault;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(PlatinumVaultMod.MOD_ID)
public class PlatinumVaultMod {
	public static final String MOD_ID = "platinumvault";

	public PlatinumVaultMod(IEventBus modBus) {
		ModRegistry.register(modBus);
		modBus.addListener(PlatinumVaultMod::registerCapabilities);
		if (FMLEnvironment.dist == Dist.CLIENT)
			ClientSetup.init();
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModRegistry.PLATINUM_VAULT_BE.get(),
			(be, side) -> be.getItemCapability());
	}
}
