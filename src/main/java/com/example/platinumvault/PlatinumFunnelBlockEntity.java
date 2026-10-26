package com.example.platinumvault;

import java.lang.reflect.Field;
import java.util.List;

import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.content.logistics.funnel.FunnelFilterSlotPositioning;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Блок-сущность воронки Create. Отличие от латунной одно: фильтр включён всегда,
 * а настраивается окном на 9 предметов, как предмет «Фильтр».
 */
public class PlatinumFunnelBlockEntity extends FunnelBlockEntity implements MenuProvider {

	public PlatinumFunnelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);

		int index = -1;
		for (int i = 0; i < behaviours.size(); i++)
			if (behaviours.get(i) instanceof FilteringBehaviour) {
				index = i;
				break;
			}
		if (index < 0)
			return;

		// своя версия фильтра: всегда активна и открывает окно по короткому клику
		FilteringBehaviour filtering = new PlatinumFilteringBehaviour(this, new FunnelFilterSlotPositioning());
		filtering.showCountWhen(this::supportsAmount);
		filtering.withCallback($ -> {
			VersionedInventoryTrackerBehaviour tracker =
				BlockEntityBehaviour.get(this, VersionedInventoryTrackerBehaviour.TYPE);
			if (tracker != null)
				tracker.reset();
		});
		behaviours.set(index, filtering);

		// родительский класс держит ссылку на фильтр в приватном поле — подменяем и её
		try {
			Field field = FunnelBlockEntity.class.getDeclaredField("filtering");
			field.setAccessible(true);
			field.set(this, filtering);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Не удалось подключить фильтр платинового шлюза", e);
		}
	}

	/** Копия правила Create: количество доступно только для вывода (извлечение, толкающая воронка на ленте). */
	private boolean supportsAmount() {
		BlockState state = getBlockState();
		boolean beltFunnelSupportsAmount = false;
		if (state.getBlock() instanceof BeltFunnelBlock) {
			BeltFunnelBlock.Shape shape = state.getValue(BeltFunnelBlock.SHAPE);
			if (shape == BeltFunnelBlock.Shape.PUSHING)
				beltFunnelSupportsAmount = true;
			else
				beltFunnelSupportsAmount = BeltHelper.getSegmentBE(level, worldPosition.below()) != null;
		}
		boolean extractor = state.getBlock() instanceof FunnelBlock && state.getValue(FunnelBlock.EXTRACTING);
		return beltFunnelSupportsAmount || extractor;
	}

	public FilteringBehaviour getFiltering() {
		return BlockEntityBehaviour.get(this, FilteringBehaviour.TYPE);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.platinumvault.platinum_funnel");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new PlatinumFunnelMenu(id, inventory, this);
	}
}
