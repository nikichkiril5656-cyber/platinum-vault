package com.example.platinumvault;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Окно фильтра шлюза в стиле предмета «Фильтр» Create: один ряд из 9 «призрачных» слотов
 * (предметы копируются, а не забираются) и настройки списка.
 */
public class PlatinumHatchMenu extends AbstractContainerMenu {
	public static final int BTN_WHITELIST = 0, BTN_BLACKLIST = 1, BTN_RESPECT_DATA = 2, BTN_IGNORE_DATA = 3,
		BTN_CLEAR = 4;

	private final IItemHandler filter;
	private final ContainerLevelAccess access;
	@Nullable
	private final PlatinumHatchBlockEntity hatch;

	public boolean blacklist;
	public boolean respectData;

	/** Клиентская сторона. */
	public PlatinumHatchMenu(int id, Inventory inventory) {
		this(id, inventory, new ItemStackHandler(PlatinumHatchBlockEntity.FILTER_SLOTS), ContainerLevelAccess.NULL,
			null);
	}

	/** Серверная сторона. */
	public PlatinumHatchMenu(int id, Inventory inventory, PlatinumHatchBlockEntity hatch) {
		this(id, inventory, hatch.getFilter(), ContainerLevelAccess.create(hatch.getLevel(), hatch.getBlockPos()),
			hatch);
		blacklist = hatch.isBlacklist();
		respectData = hatch.isRespectData();
	}

	private PlatinumHatchMenu(int id, Inventory inventory, IItemHandler filter, ContainerLevelAccess access,
		@Nullable PlatinumHatchBlockEntity hatch) {
		super(ModRegistry.PLATINUM_HATCH_MENU.get(), id);
		this.filter = filter;
		this.access = access;
		this.hatch = hatch;

		// такие же отступы, как у окна «Фильтр» Create, но один ряд вместо двух
		for (int col = 0; col < 9; col++)
			addSlot(new SlotItemHandler(filter, col, 23 + col * 18, 25));

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

	private boolean isFilterSlot(int index) {
		return index >= 0 && index < PlatinumHatchBlockEntity.FILTER_SLOTS;
	}

	private boolean hasItem(ItemStack stack) {
		for (int i = 0; i < PlatinumHatchBlockEntity.FILTER_SLOTS; i++)
			if (ItemStack.isSameItem(slots.get(i).getItem(), stack))
				return true;
		return false;
	}

	private void clearFilter() {
		for (int i = 0; i < PlatinumHatchBlockEntity.FILTER_SLOTS; i++)
			slots.get(i).set(ItemStack.EMPTY);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		switch (id) {
			case BTN_WHITELIST -> blacklist = false;
			case BTN_BLACKLIST -> blacklist = true;
			case BTN_RESPECT_DATA -> respectData = true;
			case BTN_IGNORE_DATA -> respectData = false;
			case BTN_CLEAR -> clearFilter();
			default -> {
				return false;
			}
		}
		if (hatch != null) {
			hatch.setBlacklist(blacklist);
			hatch.setRespectData(respectData);
		}
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
		if (carried.isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else if (!hasItem(carried)) {
			slot.set(carried.copyWithCount(1));
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (isFilterSlot(index)) {
			slot.set(ItemStack.EMPTY);
		} else if (slot.hasItem() && !hasItem(slot.getItem())) {
			for (int i = 0; i < PlatinumHatchBlockEntity.FILTER_SLOTS; i++) {
				Slot target = slots.get(i);
				if (!target.hasItem()) {
					target.set(slot.getItem().copyWithCount(1));
					break;
				}
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, ModRegistry.PLATINUM_HATCH.get());
	}
}
