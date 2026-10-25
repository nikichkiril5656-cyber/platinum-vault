package com.example.platinumvault;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Окно как у предмета «Фильтр» Create, но с одним рядом из 9 слотов. */
public class PlatinumHatchScreen extends AbstractSimiContainerScreen<PlatinumHatchMenu> {
	private static final int BG_WIDTH = 214;
	private static final int BG_HEIGHT = 81; // 99 у оригинала минус один ряд (18)
	private static final int TOP_PART = 43; // заголовок + первый ряд
	private static final int BOTTOM_FROM = 61; // с этой строки текстуры идёт нижняя часть
	private static final String PREFIX = "gui.filter.";

	private IconButton whitelist, blacklist, respectData, ignoreData;
	private List<Rect2i> extraAreas = Collections.emptyList();

	public PlatinumHatchScreen(PlatinumHatchMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		setWindowSize(Math.max(BG_WIDTH, PLAYER_INVENTORY.getWidth()), BG_HEIGHT + 4 + PLAYER_INVENTORY.getHeight());
		setWindowOffset(-11, 5);
		super.init();

		int x = leftPos;
		int y = topPos;
		int buttonsY = y + BG_HEIGHT - 24;

		blacklist = new IconButton(x + 18, buttonsY, AllIcons.I_BLACKLIST);
		blacklist.withCallback(() -> press(PlatinumHatchMenu.BTN_BLACKLIST, true, null));
		blacklist.setToolTip(CreateLang.translateDirect(PREFIX + "deny_list"));
		whitelist = new IconButton(x + 36, buttonsY, AllIcons.I_WHITELIST);
		whitelist.withCallback(() -> press(PlatinumHatchMenu.BTN_WHITELIST, false, null));
		whitelist.setToolTip(CreateLang.translateDirect(PREFIX + "allow_list"));

		respectData = new IconButton(x + 60, buttonsY, AllIcons.I_RESPECT_NBT);
		respectData.withCallback(() -> press(PlatinumHatchMenu.BTN_RESPECT_DATA, null, true));
		respectData.setToolTip(CreateLang.translateDirect(PREFIX + "respect_data"));
		ignoreData = new IconButton(x + 78, buttonsY, AllIcons.I_IGNORE_NBT);
		ignoreData.withCallback(() -> press(PlatinumHatchMenu.BTN_IGNORE_DATA, null, false));
		ignoreData.setToolTip(CreateLang.translateDirect(PREFIX + "ignore_data"));

		IconButton reset = new IconButton(x + BG_WIDTH - 62, buttonsY, AllIcons.I_TRASH);
		reset.withCallback(() -> sendButton(PlatinumHatchMenu.BTN_CLEAR));
		IconButton confirm = new IconButton(x + BG_WIDTH - 33, buttonsY, AllIcons.I_CONFIRM);
		confirm.withCallback(() -> minecraft.player.closeContainer());

		addRenderableWidgets(blacklist, whitelist, respectData, ignoreData, reset, confirm);
		extraAreas = ImmutableList.of(new Rect2i(x + BG_WIDTH, y + BG_HEIGHT - 40, 80, 48));
		handleIndicators();
	}

	private void press(int buttonId, Boolean newBlacklist, Boolean newRespectData) {
		if (newBlacklist != null)
			menu.blacklist = newBlacklist;
		if (newRespectData != null)
			menu.respectData = newRespectData;
		sendButton(buttonId);
		handleIndicators();
	}

	private void sendButton(int buttonId) {
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
	}

	private void handleIndicators() {
		for (IconButton button : Arrays.asList(blacklist, whitelist, respectData, ignoreData))
			button.green = !isButtonEnabled(button);
	}

	private boolean isButtonEnabled(IconButton button) {
		if (button == blacklist)
			return !menu.blacklist;
		if (button == whitelist)
			return menu.blacklist;
		if (button == respectData)
			return !menu.respectData;
		if (button == ignoreData)
			return menu.respectData;
		return true;
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		handleIndicators();
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		int invX = getLeftOfCentered(PLAYER_INVENTORY.getWidth());
		int invY = topPos + BG_HEIGHT + 4;
		renderPlayerInventory(graphics, invX, invY);

		int x = leftPos;
		int y = topPos;

		// фон «Фильтра» без второго ряда: верх + низ
		graphics.blit(AllGuiTextures.FILTER.location, x, y, 0, 0, BG_WIDTH, TOP_PART);
		graphics.blit(AllGuiTextures.FILTER.location, x, y + TOP_PART, 0, BOTTOM_FROM, BG_WIDTH,
			BG_HEIGHT - TOP_PART);

		graphics.drawString(font, title, x + (BG_WIDTH - 8) / 2 - font.width(title) / 2, y + 4, 0x303030, false);

		GuiGameElement.of(new ItemStack(ModRegistry.PLATINUM_HATCH_ITEM.get()))
			.<GuiGameElement.GuiRenderBuilder>at(x + BG_WIDTH + 8, y + BG_HEIGHT - 52, -200)
			.scale(4)
			.render(graphics);
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}
}
