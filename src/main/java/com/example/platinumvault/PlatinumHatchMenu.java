package com.example.platinumvault;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Окно фильтра шлюза: 9 «призрачных» слотов — предметы не забираются, а копируются. */
public class PlatinumHatchMenu extends AbstractContainerMenu {
	private final IItemHandler filter;
	private final ContainerLevelAccess access;

	/** Клиентская сторона. */
	public PlatinumHatchMenu(int id, Inventory inventory) {
		this(id, inventory, new ItemStackHandler(PlatinumHatchBlockEntity.FILTER_SLOTS), ContainerLevelAccess.NULL);
	}

	public PlatinumHatchMenu(int id, Inventory inventory, IItemHandler filter, ContainerLevelAccess access) {
		super(ModRegistry.PLATINUM_HATCH_MENU.get(), id);
		this.filter = filter;
		this.access = access;

		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 3; col++)
				addSlot(new SlotItemHandler(filter, col + row * 3, 62 + col * 18, 17 + row * 18));

		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++)
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
		for (int col = 0; col < 9; col++)
			addSlot(new Slot(inventory, col, 8 + col * 18, 142));
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
