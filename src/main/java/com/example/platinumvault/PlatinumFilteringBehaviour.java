package com.example.platinumvault;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Обычный фильтр воронки Create, но короткий клик по месту фильтра открывает окно «Фильтр предметов».
 * Удержание по-прежнему открывает настройку количества, а предмет «Фильтр» в руке ставится как раньше.
 */
public class PlatinumFilteringBehaviour extends FilteringBehaviour {

	public PlatinumFilteringBehaviour(SmartBlockEntity be, ValueBoxTransform slot) {
		super(be, slot);
	}

	@Override
	public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
		ItemStack held = player.getItemInHand(hand);
		if (!canShortInteract(held))
			return;
		if (held.getItem() instanceof FilterItem) {
			super.onShortInteract(player, hand, side, hitResult);
			return;
		}
		if (getWorld().isClientSide())
			return;
		if (player instanceof ServerPlayer serverPlayer && blockEntity instanceof PlatinumFunnelBlockEntity be)
			serverPlayer.openMenu(be);
	}
}
