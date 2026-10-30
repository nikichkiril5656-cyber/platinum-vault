package com.example.platinumvault;

import java.util.List;

import com.mrh0.createaddition.shapes.CAShapes;
import com.reggarf.mods.create_better_motors.config.CommonConfig;
import com.reggarf.mods.create_better_motors.util.StringFormattingTool;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Платиновый генератор переменного тока для Create: Better Motors.
 * Устроен как латунный генератор (по его образцу, лицензия MIT), но мощнее в
 * {@link PlatinumAlternatorBlockEntity#MULTIPLIER} раза.
 */
public class PlatinumAlternatorBlock extends DirectionalKineticBlock implements IBE<PlatinumAlternatorBlockEntity>, IRotate {

	public static final VoxelShaper ALTERNATOR_SHAPE = CAShapes.shape(0, 2, 0, 16, 14, 16)
		.add(1, 1, 1, 15, 15, 15)
		.forDirectional();

	public PlatinumAlternatorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return ALTERNATOR_SHAPE.get(state.getValue(FACING));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
		TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);

		if (Screen.hasShiftDown()) {
			tooltip.add(CreateLang.translate("create_better_motors.large_connector.tooltip.heavy")
				.style(ChatFormatting.AQUA)
				.component());
			return;
		}

		double fePerTick = PlatinumAlternatorBlockEntity.scaled(CommonConfig.BRASS_ALTERNATOR.FE_RPM.get())
			* CommonConfig.BRASS_ALTERNATOR.EFFICIENCY.get();

		tooltip.add(CreateLang.translate("tooltip.create_better_motors.generates")
			.style(ChatFormatting.GRAY)
			.component());
		tooltip.add(CreateLang.text(" ")
			.add(CreateLang.number(fePerTick)
				.text(" ")
				.translate("tooltip.create_better_motors.energy_per_tick")
				.style(ChatFormatting.AQUA))
			.component());
		tooltip.add(CreateLang.translate("tooltip.create_better_motors.stores")
			.style(ChatFormatting.GRAY)
			.component());
		tooltip.add(CreateLang.text(" ")
			.translate("tooltip.create_better_motors.energy", StringFormattingTool
				.formatLong(PlatinumAlternatorBlockEntity.scaled(CommonConfig.BRASS_ALTERNATOR.CAPACITY.get())))
			.style(ChatFormatting.AQUA)
			.component());
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction preferred = getPreferredFacing(context);
		if ((context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) || preferred == null)
			return super.getStateForPlacement(context);
		return defaultBlockState().setValue(FACING, preferred);
	}

	@Override
	public boolean hideStressImpact() {
		return false;
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == state.getValue(FACING).getAxis();
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING).getAxis();
	}

	@Override
	public SpeedLevel getMinimumRequiredSpeedLevel() {
		return SpeedLevel.MEDIUM;
	}

	@Override
	public BlockEntityType<? extends PlatinumAlternatorBlockEntity> getBlockEntityType() {
		return AlternatorModule.PLATINUM_ALTERNATOR_BE.get();
	}

	@Override
	public Class<PlatinumAlternatorBlockEntity> getBlockEntityClass() {
		return PlatinumAlternatorBlockEntity.class;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return AlternatorModule.PLATINUM_ALTERNATOR_BE.get().create(pos, state);
	}

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos,
		boolean isMoving) {
		if (world.getBlockEntity(pos) instanceof PlatinumAlternatorBlockEntity alternator)
			alternator.updateCache();
	}
}
