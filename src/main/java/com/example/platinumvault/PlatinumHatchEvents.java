package com.example.platinumvault;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

/** Как и у шлюза Create: правый клик срабатывает даже при зажатом шифте. */
@EventBusSubscriber(modid = PlatinumVaultMod.MOD_ID)
public class PlatinumHatchEvents {
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void useOnHatchIgnoresSneak(RightClickBlock event) {
		if (event.getUseItem() == TriState.DEFAULT
			&& event.getLevel().getBlockState(event.getPos()).getBlock() instanceof PlatinumHatchBlock)
			event.setUseBlock(TriState.TRUE);
	}
}
