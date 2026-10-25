package com.example.platinumvault;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.registry.CreateRegistries;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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

	private static final DeferredRegister<MountedItemStorageType<?>> MOUNTED_STORAGE_TYPES =
		DeferredRegister.create(CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE, PlatinumVaultMod.MOD_ID);

	public static final DeferredHolder<MountedItemStorageType<?>, PlatinumVaultMountedStorageType> PLATINUM_VAULT_MOUNTED_STORAGE =
		MOUNTED_STORAGE_TYPES.register("platinum_item_vault", PlatinumVaultMountedStorageType::new);

	private static final DeferredRegister<CreativeModeTab> TABS =
		DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PlatinumVaultMod.MOD_ID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register("main",
		() -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.platinumvault.main"))
			.icon(() -> new ItemStack(ModRegistry.PLATINUM_VAULT_ITEM.get()))
			.displayItems((params, output) -> {
				output.accept(ModRegistry.PLATINUM_VAULT_ITEM.get());
				output.accept(ModRegistry.PLATINUM_HATCH_ITEM.get());
			})
			.build());

	private static final DeferredRegister<MenuType<?>> MENUS =
		DeferredRegister.create(Registries.MENU, PlatinumVaultMod.MOD_ID);

	public static final DeferredHolder<MenuType<?>, MenuType<PlatinumHatchMenu>> PLATINUM_HATCH_MENU =
		MENUS.register("platinum_hatch", () -> IMenuTypeExtension.create((id, inv, buf) -> new PlatinumHatchMenu(id, inv)));

	public static final DeferredBlock<PlatinumHatchBlock> PLATINUM_HATCH = BLOCKS.register("platinum_hatch",
		() -> new PlatinumHatchBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_LIGHT_BLUE)
			.strength(3.0f, 6f)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.sound(SoundType.NETHERITE_BLOCK)));

	public static final DeferredItem<net.minecraft.world.item.BlockItem> PLATINUM_HATCH_ITEM = ITEMS.register("platinum_hatch",
		() -> new net.minecraft.world.item.BlockItem(PLATINUM_HATCH.get(), new Item.Properties()));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlatinumHatchBlockEntity>> PLATINUM_HATCH_BE =
		BLOCK_ENTITIES.register("platinum_hatch",
			() -> BlockEntityType.Builder.of(
				(pos, state) -> new PlatinumHatchBlockEntity(ModRegistry.PLATINUM_HATCH_BE.get(), pos, state),
				PLATINUM_HATCH.get()).build(null));

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
		MOUNTED_STORAGE_TYPES.register(bus);
		TABS.register(bus);
		MENUS.register(bus);
		bus.addListener(ModRegistry::addToTabs);
	}

	private static void addToTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
			event.accept(PLATINUM_VAULT_ITEM.get());
	}
}
