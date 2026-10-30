package com.example.platinumvault;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mrh0.createaddition.energy.IEnergyProvider;
import com.mrh0.createaddition.energy.InternalEnergyStorage;
import com.mrh0.createaddition.sound.CASoundScapes;
import com.reggarf.mods.create_better_motors.config.CommonConfig;
import com.reggarf.mods.create_better_motors.registry.CBMBlocks;
import com.reggarf.mods.create_better_motors.util.StringFormattingTool;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Платиновый генератор переменного тока: превращает вращение Create в энергию (FE).
 * Логика повторяет латунный генератор Create: Better Motors, но все показатели берутся из его
 * конфига латунного генератора и умножаются на {@link #MULTIPLIER}: буфер, выдача, FE на оборот и нагрузка.
 */
public class PlatinumAlternatorBlockEntity extends KineticBlockEntity implements IEnergyProvider {

	/** Во сколько раз платиновый генератор мощнее латунного. */
	public static final int MULTIPLIER = 2;

	protected final InternalEnergyStorage energy;

	private final EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> cache =
		new EnumMap<>(Direction.class);
	private final EnumSet<Direction> invalidSides = EnumSet.noneOf(Direction.class);
	private boolean firstTickState = true;

	public PlatinumAlternatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		energy = new InternalEnergyStorage(scaled(CommonConfig.BRASS_ALTERNATOR.CAPACITY.get()), 0,
			scaled(CommonConfig.BRASS_ALTERNATOR.MAX_OUTPUT.get()));
	}

	/** Значение латунного генератора, умноженное на {@link #MULTIPLIER} без переполнения int. */
	public static int scaled(int brassValue) {
		return (int) Math.min((long) brassValue * MULTIPLIER, Integer.MAX_VALUE);
	}

	public static int getEnergyProductionRate(int rpm) {
		rpm = Math.abs(rpm);
		return (int) (scaled(CommonConfig.BRASS_ALTERNATOR.FE_RPM.get()) * ((double) rpm / 256d)
			* CommonConfig.BRASS_ALTERNATOR.EFFICIENCY.get());
	}

	/** Нужно при улучшении латунного генератора: переносит накопленную энергию. */
	public void setStoredEnergy(int stored) {
		energy.setEnergy(Math.min(stored, energy.getMaxEnergyStored()));
		setChanged();
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;

		if (firstTickState) {
			updateCache();
			firstTickState = false;
		}

		boolean produced = false;
		if (Math.abs(getSpeed()) > 0 && isSpeedRequirementFulfilled()) {
			energy.internalProduceEnergy(getEnergyProductionRate((int) getSpeed()));
			produced = true;
		}

		int maxOutput = scaled(CommonConfig.BRASS_ALTERNATOR.MAX_OUTPUT.get());
		for (Direction side : Direction.values()) {
			BlockCapabilityCache<IEnergyStorage, Direction> capability = cache.get(side);
			if (capability == null)
				continue;
			IEnergyStorage target = capability.getCapability();
			if (target == null)
				continue;

			int accepted = target.receiveEnergy(maxOutput, true);
			int extracted = energy.extractEnergy(accepted, false);
			target.receiveEnergy(extracted, false);
			if (extracted > 0)
				produced = true;
		}

		if (produced)
			sendData();
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		CreateLang.translate("tooltip.create_better_motors.generating")
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);
		CreateLang.translate("tooltip.create_better_motors.energy_output")
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);
		CreateLang.translate("tooltip.create_better_motors.energy_per_tick",
				StringFormattingTool.formatLong(getEnergyProductionRate((int) getSpeed())))
			.style(ChatFormatting.AQUA)
			.forGoggles(tooltip, 1);
		CreateLang.translate("tooltip.create_better_motors.energy_stored")
			.style(ChatFormatting.WHITE)
			.forGoggles(tooltip);
		CreateLang.translate("tooltip.create_better_motors.energy_storage",
				StringFormattingTool.formatLong(energy.getEnergyStored()),
				StringFormattingTool.formatLong(energy.getMaxEnergyStored()))
			.style(ChatFormatting.AQUA)
			.forGoggles(tooltip, 1);

		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	@Override
	public float calculateStressApplied() {
		float impact = scaled(CommonConfig.BRASS_ALTERNATOR.MAX_STRESS.get()) / 256f;
		lastStressApplied = impact;
		return impact;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void tickAudio() {
		super.tickAudio();
		float componentSpeed = Math.abs(getSpeed());
		if (componentSpeed == 0 || !isSpeedRequirementFulfilled())
			return;
		if (CommonConfig.BRASS_ALTERNATOR.AUDIO_ENABLED.get()) {
			float pitch = Mth.clamp((componentSpeed / 256f) + .5f, .5f, 1.5f);
			CASoundScapes.play(CASoundScapes.AmbienceGroup.DYNAMO, worldPosition, pitch);
		}
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		energy.read(tag);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		energy.write(tag);
	}

	@Override
	public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
		super.writeSafe(tag, registries);
		energy.write(tag);
	}

	/** Кэширует соседние блоки, в которые можно отдавать энергию. */
	public void updateCache() {
		if (!(level instanceof ServerLevel server))
			return;
		cache.clear();
		invalidSides.clear();
		for (Direction side : Direction.values())
			cache.put(side, BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, server,
				worldPosition.relative(side), side.getOpposite(), () -> !this.isRemoved(),
				() -> invalidSides.add(side)));
	}

	@Override
	protected Block getStressConfigKey() {
		// значения нагрузки у Better Motors заведены по блоку латунного генератора
		return CBMBlocks.BRASS_ALTERNATOR.get();
	}

	@Override
	public IEnergyStorage getEnergyStorage(@Nullable Direction direction) {
		return energy;
	}
}
