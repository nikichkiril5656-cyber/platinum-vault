package com.example.platinumvault;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Логика предмета «Платиновое улучшение»: заменяет блоки Create на платиновые, сохраняя содержимое.
 *
 * <ul>
 * <li>жидкостный бак Create → платиновый бак из Create Propulsion (жидкость переносится);</li>
 * <li>латунный шлюз (воронка), в том числе на ленте → платиновый шлюз (фильтр переносится);</li>
 * <li>хранилище предметов Create → платиновое хранилище (предметы остаются на своих местах).</li>
 * </ul>
 *
 * Баки и хранилища улучшаются целиком: один клик по мультиблоку заменяет все его блоки,
 * на каждый блок уходит одно улучшение (в творческом режиме предметы не тратятся).
 */
public final class PlatinumUpgrades {

	/** Платиновый бак живёт в Create Propulsion, поэтому ищем его по идентификатору, а не по классу. */
	private static final ResourceLocation PLATINUM_TANK_ID =
		ResourceLocation.fromNamespaceAndPath("createpropulsion", "platinum_fluid_tank");

	public enum Kind {
		TANK, VAULT, FUNNEL
	}

	private PlatinumUpgrades() {}

	/** Что это за блок с точки зрения улучшения, или null, если улучшать его нечем. */
	@Nullable
	public static Kind kindOf(BlockState state) {
		if (AllBlocks.FLUID_TANK.has(state))
			return Kind.TANK;
		if (AllBlocks.ITEM_VAULT.has(state))
			return Kind.VAULT;
		if (AllBlocks.BRASS_FUNNEL.has(state) || AllBlocks.BRASS_BELT_FUNNEL.has(state))
			return Kind.FUNNEL;
		return null;
	}

	/** Улучшает блок (или весь мультиблок) под курсором. Возвращает true, если блоки были заменены. */
	public static boolean upgrade(ServerLevel level, BlockPos pos, Player player, ItemStack stack) {
		BlockState state = level.getBlockState(pos);
		Kind kind = kindOf(state);
		if (kind == null || !player.mayBuild())
			return false;
		return switch (kind) {
			case TANK -> upgradeTank(level, pos, state, player, stack);
			case VAULT -> upgradeVault(level, pos, state, player, stack);
			case FUNNEL -> upgradeFunnel(level, pos, state, player, stack);
		};
	}

	// ---- бак ----

	private static boolean upgradeTank(ServerLevel level, BlockPos pos, BlockState state, Player player,
		ItemStack stack) {
		Optional<Block> target = BuiltInRegistries.BLOCK.getOptional(PLATINUM_TANK_ID);
		if (target.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.platinumvault.upgrade.no_tank"), true);
			return false;
		}

		FluidTankBlockEntity part = ConnectivityHandler.partAt(AllBlockEntityTypes.FLUID_TANK.get(), level, pos);
		FluidTankBlockEntity controller = part == null ? null : part.getControllerBE();
		BlockPos origin = controller == null ? pos : controller.getBlockPos();
		int width = controller == null ? 1 : controller.getWidth();
		int height = controller == null ? 1 : controller.getHeight();

		// все блоки этого бака; у мультиблока контроллер стоит в углу с наименьшими координатами
		List<BlockPos> positions = new ArrayList<>();
		List<BlockState> oldStates = new ArrayList<>();
		boolean window = false;
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++) {
					BlockPos p = origin.offset(x, y, z);
					BlockState s = level.getBlockState(p);
					if (!AllBlocks.FLUID_TANK.has(s))
						continue;
					positions.add(p);
					oldStates.add(s);
					window |= s.getValue(FluidTankBlock.SHAPE) != FluidTankBlock.Shape.PLAIN;
				}
		if (!positions.contains(pos)) {
			// на случай рассинхронизации данных мультиблока: улучшаем только нажатый блок
			positions.clear();
			oldStates.clear();
			positions.add(pos);
			oldStates.add(state);
			origin = pos;
			controller = null;
		}

		if (!canEdit(level, player, positions) || !hasEnough(player, stack, positions.size()))
			return false;

		FluidStack fluid = controller == null ? FluidStack.EMPTY : controller.getTankInventory().getFluid().copy();

		BlockState base = target.get().defaultBlockState();
		for (int i = 0; i < positions.size(); i++)
			level.setBlock(positions.get(i), copyProperties(oldStates.get(i), base), Block.UPDATE_ALL);

		// последний в списке — угол с наибольшими координатами: отсюда поиск соседей доходит до всех блоков
		if (level.getBlockEntity(positions.get(positions.size() - 1)) instanceof FluidTankBlockEntity tank)
			ConnectivityHandler.formMulti(tank);

		if (level.getBlockEntity(origin) instanceof FluidTankBlockEntity first) {
			FluidTankBlockEntity newController = first.getControllerBE();
			if (newController != null) {
				if (!fluid.isEmpty())
					newController.getTankInventory().fill(fluid, FluidAction.EXECUTE);
				if (!window)
					newController.setWindows(false);
			}
		}

		finish(level, player, stack, positions);
		return true;
	}

	// ---- хранилище ----

	private static boolean upgradeVault(ServerLevel level, BlockPos pos, BlockState state, Player player,
		ItemStack stack) {
		ItemVaultBlockEntity part = ConnectivityHandler.partAt(AllBlockEntityTypes.ITEM_VAULT.get(), level, pos);
		ItemVaultBlockEntity controller = part == null ? null : part.getControllerBE();
		BlockPos origin = controller == null ? pos : controller.getBlockPos();
		Direction.Axis axis = controller == null ? Direction.Axis.X : controller.getMainConnectionAxis();
		int width = controller == null ? 1 : controller.getWidth();
		int length = controller == null ? 1 : controller.getHeight();

		List<BlockPos> positions = new ArrayList<>();
		List<BlockState> oldStates = new ArrayList<>();
		for (int l = 0; l < length; l++)
			for (int a = 0; a < width; a++)
				for (int b = 0; b < width; b++) {
					BlockPos p = switch (axis) {
						case X -> origin.offset(l, a, b);
						case Y -> origin.offset(a, l, b);
						case Z -> origin.offset(a, b, l);
					};
					BlockState s = level.getBlockState(p);
					if (!AllBlocks.ITEM_VAULT.has(s))
						continue;
					positions.add(p);
					oldStates.add(s);
				}
		if (!positions.contains(pos)) {
			positions.clear();
			oldStates.clear();
			positions.add(pos);
			oldStates.add(state);
			origin = pos;
		}

		if (!canEdit(level, player, positions) || !hasEnough(player, stack, positions.size()))
			return false;

		// забираем предметы заранее: обычное хранилище при замене выбросило бы всё содержимое на землю
		List<List<ItemStack>> contents = new ArrayList<>();
		for (BlockPos p : positions) {
			List<ItemStack> saved = new ArrayList<>();
			if (level.getBlockEntity(p) instanceof ItemVaultBlockEntity vault) {
				ItemStackHandler inventory = vault.getInventoryOfBlock();
				for (int slot = 0; slot < inventory.getSlots(); slot++) {
					saved.add(inventory.getStackInSlot(slot).copy());
					inventory.setStackInSlot(slot, ItemStack.EMPTY);
				}
			}
			contents.add(saved);
		}

		BlockState base = ModRegistry.PLATINUM_VAULT.get().defaultBlockState();
		for (int i = 0; i < positions.size(); i++)
			level.setBlock(positions.get(i), copyProperties(oldStates.get(i), base), Block.UPDATE_ALL);

		if (level.getBlockEntity(positions.get(positions.size() - 1)) instanceof PlatinumVaultBlockEntity vault)
			ConnectivityHandler.formMulti(vault);

		// предметы возвращаются в те же блоки и те же слоты (слотов у платинового хранилища больше)
		for (int i = 0; i < positions.size(); i++) {
			BlockPos p = positions.get(i);
			PlatinumVaultBlockEntity vault =
				level.getBlockEntity(p) instanceof PlatinumVaultBlockEntity v ? v : null;
			ItemStackHandler inventory = vault == null ? null : vault.getInventoryOfBlock();
			List<ItemStack> saved = contents.get(i);
			for (int slot = 0; slot < saved.size(); slot++) {
				ItemStack item = saved.get(slot);
				if (item.isEmpty())
					continue;
				if (inventory != null && slot < inventory.getSlots())
					inventory.setStackInSlot(slot, item);
				else
					Containers.dropItemStack(level, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, item);
			}
		}

		finish(level, player, stack, positions);
		return true;
	}

	// ---- шлюз ----

	private static boolean upgradeFunnel(ServerLevel level, BlockPos pos, BlockState state, Player player,
		ItemStack stack) {
		List<BlockPos> positions = List.of(pos);
		if (!canEdit(level, player, positions) || !hasEnough(player, stack, 1))
			return false;

		// фильтр латунного шлюза переезжает в платиновый
		ItemStack filter = ItemStack.EMPTY;
		if (level.getBlockEntity(pos) instanceof FunnelBlockEntity funnel) {
			FilteringBehaviour filtering = BlockEntityBehaviour.get(funnel, FilteringBehaviour.TYPE);
			if (filtering != null)
				filter = filtering.getFilter().copy();
		}

		Block target = state.getBlock() instanceof BeltFunnelBlock ? ModRegistry.PLATINUM_BELT_FUNNEL.get()
			: ModRegistry.PLATINUM_FUNNEL.get();
		level.setBlock(pos, copyProperties(state, target.defaultBlockState()), Block.UPDATE_ALL);

		if (!filter.isEmpty() && level.getBlockEntity(pos) instanceof PlatinumFunnelBlockEntity platinum)
			platinum.getFiltering().setFilter(filter);

		finish(level, player, stack, positions);
		return true;
	}

	// ---- общее ----

	private static boolean canEdit(ServerLevel level, Player player, List<BlockPos> positions) {
		for (BlockPos p : positions)
			if (!level.mayInteract(player, p))
				return false;
		return true;
	}

	private static boolean hasEnough(Player player, ItemStack stack, int needed) {
		if (player.isCreative() || stack.getCount() >= needed)
			return true;
		player.displayClientMessage(Component.translatable("message.platinumvault.upgrade.not_enough", needed), true);
		return false;
	}

	private static void finish(ServerLevel level, Player player, ItemStack stack, List<BlockPos> positions) {
		if (!player.isCreative())
			stack.shrink(positions.size());
		level.playSound(null, positions.get(0), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
		for (BlockPos p : positions)
			level.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3,
				0.3, 0.02);
	}

	/**
	 * Переносит свойства блока (ось, направление, питание и т.д.) на новый блок. Сопоставление идёт по имени
	 * свойства и значения, потому что у платиновых блоков часть свойств объявлена заново.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static BlockState copyProperties(BlockState from, BlockState to) {
		for (Property<?> source : from.getProperties()) {
			Property target = to.getBlock().getStateDefinition().getProperty(source.getName());
			if (target == null)
				continue;
			Comparable value = from.getValue(source);
			Optional parsed = target.getValue(((Property) source).getName(value));
			if (parsed.isPresent())
				to = (BlockState) to.setValue(target, (Comparable) parsed.get());
		}
		return to;
	}
}
