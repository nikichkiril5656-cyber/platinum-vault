package com.example.platinumvault;

import java.util.List;

import com.reggarf.mods.create_better_motors.content.alternator.blocksentity.BrassAlternatorBlockEntity;
import com.reggarf.mods.create_better_motors.registry.CBMBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Платиновый генератор переменного тока для Create: Better Motors.
 * Этот класс загружается только если установлены Better Motors и Create Crafts &amp; Additions,
 * поэтому без них мод работает как раньше.
 */
public final class AlternatorModule {
	public static final String BETTER_MOTORS = "create_better_motors";
	public static final String CREATE_ADDITION = "createaddition";

	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PlatinumVaultMod.MOD_ID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PlatinumVaultMod.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
		DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PlatinumVaultMod.MOD_ID);

	public static final DeferredBlock<PlatinumAlternatorBlock> PLATINUM_ALTERNATOR =
		BLOCKS.register("platinum_alternator",
			() -> new PlatinumAlternatorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK)));

	public static final DeferredItem<BlockItem> PLATINUM_ALTERNATOR_ITEM = ITEMS.register("platinum_alternator",
		() -> new BlockItem(PLATINUM_ALTERNATOR.get(), new Item.Properties()));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlatinumAlternatorBlockEntity>> PLATINUM_ALTERNATOR_BE =
		BLOCK_ENTITIES.register("platinum_alternator",
			() -> BlockEntityType.Builder.of(
				(pos, state) -> new PlatinumAlternatorBlockEntity(AlternatorModule.PLATINUM_ALTERNATOR_BE.get(), pos,
					state),
				PLATINUM_ALTERNATOR.get()).build(null));

	private AlternatorModule() {}

	public static void init(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		BLOCK_ENTITIES.register(modBus);
		modBus.addListener(AlternatorModule::registerCapabilities);
		modBus.addListener(AlternatorModule::addToTabs);

		// ПКМ платиновым улучшением по латунному генератору
		PlatinumUpgrades.register(new BrassAlternatorUpgrade());

		if (FMLEnvironment.dist == Dist.CLIENT)
			AlternatorClient.init(modBus);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PLATINUM_ALTERNATOR_BE.get(),
			(be, side) -> be.getEnergyStorage(side));
	}

	private static void addToTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == ModRegistry.MAIN_TAB.getKey()
			|| event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS)
			event.accept(PLATINUM_ALTERNATOR_ITEM.get());
	}

	/** Улучшение латунного генератора до платинового: накопленная энергия и направление сохраняются. */
	private static final class BrassAlternatorUpgrade implements PlatinumUpgrades.Extension {
		@Override
		public boolean matches(BlockState state) {
			return CBMBlocks.BRASS_ALTERNATOR.has(state);
		}

		@Override
		public boolean upgrade(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack stack) {
			List<BlockPos> positions = List.of(pos);
			if (!PlatinumUpgrades.canEdit(level, player, positions) || !PlatinumUpgrades.hasEnough(player, stack, 1))
				return false;

			int stored = 0;
			if (level.getBlockEntity(pos) instanceof BrassAlternatorBlockEntity brass)
				stored = brass.getEnergyStorage(null).getEnergyStored();

			Block target = PLATINUM_ALTERNATOR.get();
			level.setBlock(pos, PlatinumUpgrades.copyProperties(state, target.defaultBlockState()),
				Block.UPDATE_ALL);

			if (stored > 0 && level.getBlockEntity(pos) instanceof PlatinumAlternatorBlockEntity platinum)
				platinum.setStoredEnergy(stored);

			PlatinumUpgrades.finish(level, player, stack, positions);
			return true;
		}
	}
}
