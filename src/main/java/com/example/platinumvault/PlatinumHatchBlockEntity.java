package com.example.platinumvault;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Хранит фильтр платинового шлюза: до 9 предметов, режим белого/чёрного списка
 * и учёт данных предмета (как у предмета «Фильтр» из Create). Пустой список пропускает всё.
 */
public class PlatinumHatchBlockEntity extends BlockEntity implements MenuProvider {
	public static final int FILTER_SLOTS = 9;

	private final ItemStackHandler filter = new ItemStackHandler(FILTER_SLOTS) {
		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};
	private boolean blacklist;
	private boolean respectData;

	public PlatinumHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public ItemStackHandler getFilter() {
		return filter;
	}

	public boolean isBlacklist() {
		return blacklist;
	}

	public boolean isRespectData() {
		return respectData;
	}

	public void setBlacklist(boolean value) {
		blacklist = value;
		setChanged();
	}

	public void setRespectData(boolean value) {
		respectData = value;
		setChanged();
	}

	/** Подходит ли предмет под фильтр. */
	public boolean test(ItemStack stack) {
		boolean anySet = false;
		boolean matched = false;
		for (int i = 0; i < filter.getSlots(); i++) {
			ItemStack f = filter.getStackInSlot(i);
			if (f.isEmpty())
				continue;
			anySet = true;
			if (respectData ? ItemStack.isSameItemSameComponents(f, stack) : ItemStack.isSameItem(f, stack)) {
				matched = true;
				break;
			}
		}
		if (!anySet)
			return true;
		return blacklist != matched;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("Filter", filter.serializeNBT(registries));
		tag.putBoolean("Blacklist", blacklist);
		tag.putBoolean("RespectData", respectData);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		filter.deserializeNBT(registries, tag.getCompound("Filter"));
		blacklist = tag.getBoolean("Blacklist");
		respectData = tag.getBoolean("RespectData");
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.platinumvault.platinum_hatch");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new PlatinumHatchMenu(id, inventory, this);
	}
}
