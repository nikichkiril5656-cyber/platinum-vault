package com.example.platinumvault;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRegistry {
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PlatinumVaultMod.MOD_ID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PlatinumVaultMod.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
		DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PlatinumVaultMod.MOD_ID);

	public static final DeferredBlock<PlatinumVaultBlock> PLATINUM_VAULT = BLOCKS.register("platinum_item_vault",
		() -> new PlatinumVaultBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_LIGHT_BLUE)
			.strength(4.0f, 1200f)
			.requiresCorrectToolForDrops()
			.sound(SoundType.NETHERITE_BLOCK)));

	public static final DeferredItem<PlatinumVaultItem> PLATINUM_VAULT_ITEM = ITEMS.register("platinum_item_vault",
		() -> new PlatinumVaultItem(PLATINUM_VAULT.get(), new Item.Properties()));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlatinumVaultBlockEntity>> PLATINUM_VAULT_BE =
		BLOCK_ENTITIES.register("platinum_item_vault",
			() -> BlockEntityType.Builder.of(
				(pos, state) -> new PlatinumVaultBlockEntity(ModRegistry.PLATINUM_VAULT_BE.get(), pos, state),
				PLATINUM_VAULT.get()).build(null));

	public static void register(IEventBus bus) {
		BLOCKS.register(bus);
		ITEMS.register(bus);
		BLOCK_ENTITIES.register(bus);
		bus.addListener(ModRegistry::addToTabs);
	}

	private static void addToTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
			event.accept(PLATINUM_VAULT_ITEM.get());
	}
}
