package com.example.platinumvault;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Окно фильтра воронки в стиле предмета «Фильтр» Create: один ряд из 9 «призрачных» слотов
 * (предметы копируются, а не забираются) плюс чёрный/белый список и учёт данных предмета.
 * Результат сохраняется в фильтре воронки как обычный предмет «Фильтр» со списком.
 */
public class PlatinumFunnelMenu extends AbstractContainerMenu {
	public static final int SLOTS = 9;
	public static final int BTN_WHITELIST = 0, BTN_BLACKLIST = 1, BTN_RESPECT_DATA = 2, BTN_IGNORE_DATA = 3,
		BTN_CLEAR = 4;

	private final ItemStackHandler ghost = new ItemStackHandler(SLOTS);
	@Nullable
	private final PlatinumFunnelBlockEntity be;

	public boolean blacklist;
	public boolean respectData;

	/** Клиентская сторона. */
	public PlatinumFunnelMenu(int id, Inventory inventory) {
		this(id, inventory, null);
	}

	/** Серверная сторона. */
	public PlatinumFunnelMenu(int id, Inventory inventory, @Nullable PlatinumFunnelBlockEntity be) {
		super(ModRegistry.PLATINUM_FUNNEL_MENU.get(), id);
		this.be = be;

		if (be != null)
			readFromFilter(be.getFiltering().getFilter());

		// те же отступы, что у окна «Фильтр» Create, но один ряд
		for (int col = 0; col < SLOTS; col++)
			addSlot(new SlotItemHandler(ghost, col, 23 + col * 18, 25));

		int invX = 38, invY = 103;
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++)
				addSlot(new Slot(inventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
		for (int col = 0; col < 9; col++)
			addSlot(new Slot(inventory, col, invX + col * 18, invY + 58));

		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return blacklist ? 1 : 0;
			}

			@Override
			public void set(int value) {
				blacklist = value == 1;
			}
		});
		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return respectData ? 1 : 0;
			}

			@Override
			public void set(int value) {
				respectData = value == 1;
			}
		});
	}

	private void readFromFilter(ItemStack filter) {
		if (!filter.is(AllItems.FILTER.get()))
			return;
		blacklist = filter.getOrDefault(AllDataComponents.FILTER_ITEMS_BLACKLIST, false);
		respectData = filter.getOrDefault(AllDataComponents.FILTER_ITEMS_RESPECT_NBT, false);
		int i = 0;
		for (ItemStack stack : filter.getOrDefault(AllDataComponents.FILTER_ITEMS, ItemContainerContents.EMPTY)
			.nonEmptyItems()) {
			if (i >= SLOTS)
				break;
			ghost.setStackInSlot(i++, stack.copyWithCount(1));
		}
	}

	/** Записывает настройки обратно в фильтр воронки (только на сервере). */
	private void apply() {
		if (be == null || be.getLevel() == null || be.getLevel().isClientSide())
			return;
		List<ItemStack> items = new ArrayList<>();
		boolean any = false;
		for (int i = 0; i < SLOTS; i++) {
			ItemStack s = ghost.getStackInSlot(i);
			items.add(s.copy());
			any |= !s.isEmpty();
		}
		FilteringBehaviour filtering = be.getFiltering();
		if (!any) {
			// пустой список = фильтр выключен (пропускает всё)
			filtering.setFilter(ItemStack.EMPTY);
			return;
		}
		ItemStack filter = new ItemStack(AllItems.FILTER.get());
		filter.set(AllDataComponents.FILTER_ITEMS, ItemContainerContents.fromItems(items));
		filter.set(AllDataComponents.FILTER_ITEMS_BLACKLIST, blacklist);
		filter.set(AllDataComponents.FILTER_ITEMS_RESPECT_NBT, respectData);
		filtering.setFilter(filter);
	}

	private boolean isFilterSlot(int index) {
		return index >= 0 && index < SLOTS;
	}

	private boolean hasItem(ItemStack stack) {
		for (int i = 0; i < SLOTS; i++)
			if (ItemStack.isSameItemSameComponents(ghost.getStackInSlot(i), stack))
				return true;
		return false;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		switch (id) {
			case BTN_WHITELIST -> blacklist = false;
			case BTN_BLACKLIST -> blacklist = true;
			case BTN_RESPECT_DATA -> respectData = true;
			case BTN_IGNORE_DATA -> respectData = false;
			case BTN_CLEAR -> {
				for (int i = 0; i < SLOTS; i++)
					ghost.setStackInSlot(i, ItemStack.EMPTY);
			}
			default -> {
				return false;
			}
		}
		apply();
		return true;
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (!isFilterSlot(slotId)) {
			super.clicked(slotId, button, clickType, player);
			return;
		}
		if (clickType != ClickType.PICKUP)
			return;
		Slot slot = slots.get(slotId);
		ItemStack carried = getCarried();
		if (carried.isEmpty())
			slot.set(ItemStack.EMPTY);
		else if (!hasItem(carried))
			slot.set(carried.copyWithCount(1));
		apply();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (isFilterSlot(index)) {
			slot.set(ItemStack.EMPTY);
		} else if (slot.hasItem() && !hasItem(slot.getItem())) {
			for (int i = 0; i < SLOTS; i++) {
				if (ghost.getStackInSlot(i).isEmpty()) {
					ghost.setStackInSlot(i, slot.getItem().copyWithCount(1));
					break;
				}
			}
		}
		apply();
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		if (be == null)
			return true;
		return !be.isRemoved() && player.distanceToSqr(be.getBlockPos().getCenter()) < 64;
	}
}
