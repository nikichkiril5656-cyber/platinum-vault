package com.example.platinumvault;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.itemHatch.HatchFilterSlot;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.common.Tags.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Платиновый шлюз: работает как шлюз Create, но по клику на место фильтра открывает окно
 * с 9 слотами — пропускаются предметы, совпадающие с любым из них.
 */
public class PlatinumHatchBlock extends HorizontalDirectionalBlock
	implements IBE<PlatinumHatchBlockEntity>, IWrenchable, ProperWaterloggedBlock {
	public static final MapCodec<PlatinumHatchBlock> CODEC = simpleCodec(PlatinumHatchBlock::new);
	public static final BooleanProperty OPEN = BooleanProperty.create("open");
	private static final HatchFilterSlot FILTER_SLOT = new HatchFilterSlot();

	public PlatinumHatchBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(OPEN, false).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(OPEN, FACING, WATERLOGGED));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState state = super.getStateForPlacement(ctx);
		if (state == null || ctx.getClickedFace().getAxis().isVertical())
			return null;
		return withWater(state.setValue(FACING, ctx.getClickedFace().getOpposite()).setValue(OPEN, false), ctx);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return fluidState(state);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
		BlockPos pos, BlockPos neighborPos) {
		updateWater(level, state, pos);
		return state;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hit) {
		if (player instanceof FakePlayer)
			return ItemInteractionResult.SUCCESS;
		if (stack.is(Items.TOOLS_WRENCH))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

		// клик по месту фильтра — открыть окно с 9 слотами
		Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		if (isFilterHit(level, pos, state, local)) {
			if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
				&& level.getBlockEntity(pos) instanceof PlatinumHatchBlockEntity be)
				serverPlayer.openMenu(be);
			return ItemInteractionResult.SUCCESS;
		}

		if (level.isClientSide())
			return ItemInteractionResult.SUCCESS;

		BlockEntity target = level.getBlockEntity(pos.relative(state.getValue(FACING)));
		if (target == null)
			return ItemInteractionResult.FAIL;
		IItemHandler targetInv = level.getCapability(ItemHandler.BLOCK, target.getBlockPos(), null);
		if (targetInv == null)
			return ItemInteractionResult.FAIL;
		if (!(level.getBlockEntity(pos) instanceof PlatinumHatchBlockEntity hatch))
			return ItemInteractionResult.FAIL;

		Inventory inventory = player.getInventory();
		List<ItemStack> failed = new ArrayList<>();
		boolean anyInserted = false;
		boolean depositHand = !player.isShiftKeyDown();

		for (int i = 0; i < inventory.items.size(); i++) {
			if (Inventory.isHotbarSlot(i) != depositHand)
				continue;
			if (depositHand && i != inventory.selected)
				continue;
			ItemStack item = inventory.getItem(i);
			if (item.isEmpty())
				continue;
			if (!item.getItem().canFitInsideContainerItems() && !PackageItem.isPackage(item))
				continue;
			if (!hatch.test(item))
				continue;

			ItemStack remainder = ItemHandlerHelper.insertItemStacked(targetInv, item, true);
			if (remainder.getCount() == item.getCount())
				continue;

			ItemStack extracted = inventory.removeItem(i, item.getCount() - remainder.getCount());
			remainder = ItemHandlerHelper.insertItemStacked(targetInv, extracted, false);
			anyInserted = true;
			if (!remainder.isEmpty())
				failed.add(remainder);
		}
		failed.forEach(inventory::placeItemBackInInventory);

		if (!anyInserted)
			return ItemInteractionResult.SUCCESS;

		AllSoundEvents.ITEM_HATCH.playOnServer(level, pos);
		level.setBlockAndUpdate(pos, state.setValue(OPEN, true));
		level.scheduleTick(pos, this, 10);
		CreateLang.translate(depositHand ? "item_hatch.deposit_item" : "item_hatch.deposit_inventory")
			.sendStatus(player);
		return ItemInteractionResult.SUCCESS;
	}

	/** Чуть более щедрая область, чем у значка фильтра в Create, чтобы было проще попасть. */
	private static boolean isFilterHit(Level level, BlockPos pos, BlockState state, Vec3 local) {
		if (FILTER_SLOT.testHit(level, pos, state, local))
			return true;
		Vec3 center = FILTER_SLOT.getLocalOffset(level, pos, state);
		return local.distanceTo(center.subtract(0, 0.125, 0)) < 0.3;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return AllShapes.ITEM_HATCH.get(state.getValue(FACING).getOpposite());
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(OPEN))
			level.setBlockAndUpdate(pos, state.setValue(OPEN, false));
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<PlatinumHatchBlockEntity> getBlockEntityClass() {
		return PlatinumHatchBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PlatinumHatchBlockEntity> getBlockEntityType() {
		return ModRegistry.PLATINUM_HATCH_BE.get();
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}
}
