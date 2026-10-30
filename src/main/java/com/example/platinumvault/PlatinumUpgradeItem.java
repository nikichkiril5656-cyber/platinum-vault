package com.example.platinumvault;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Платиновое улучшение. ПКМ по жидкостному баку, латунному шлюзу или хранилищу предметов Create
 * заменяет их платиновыми версиями (см. {@link PlatinumUpgrades}).
 *
 * Клик перехватывается событием с высоким приоритетом: иначе шлюз воспринял бы предмет в руке
 * как новый фильтр, а бак и хранилище открыли бы свои обычные действия.
 */
@EventBusSubscriber(modid = PlatinumVaultMod.MOD_ID)
public class PlatinumUpgradeItem extends Item {

	public PlatinumUpgradeItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
		TooltipFlag flag) {
		tooltip.add(Component.translatable("item.platinumvault.platinum_upgrade.tooltip.use")
			.withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.platinumvault.platinum_upgrade.tooltip.targets")
			.withStyle(ChatFormatting.DARK_AQUA));
		tooltip.add(Component.translatable("item.platinumvault.platinum_upgrade.tooltip.multiblock")
			.withStyle(ChatFormatting.DARK_GRAY));
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void upgradeOnRightClick(PlayerInteractEvent.RightClickBlock event) {
		ItemStack stack = event.getItemStack();
		if (!(stack.getItem() instanceof PlatinumUpgradeItem))
			return;

		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		if (PlatinumUpgrades.kindOf(level.getBlockState(pos)) == null)
			return;

		// на этих блоках улучшение всегда «занимает» клик, даже если улучшить не получилось
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
		if (level instanceof ServerLevel serverLevel)
			PlatinumUpgrades.upgrade(serverLevel, pos, event.getEntity(), stack);
	}
}
