package dev.couchlegacy;

import java.util.Set;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;
import wily.legacy.client.screen.RenderableVList;

/**
 * Like the console editions, guests only get to their own things: controls, skin, how to play,
 * leaderboards, achievements, and the player list (SELECT) with chat. Game settings, graphics, audio,
 * mods, the game/player host options and saving belong to player 1.
 */
public final class GuestScreens {
	private static final Set<String> BLOCKED = Set.of(
		// Vanilla
		"net.minecraft.client.gui.screens.options.OptionsScreen",
		"net.minecraft.client.gui.screens.options.VideoSettingsScreen",
		"net.minecraft.client.gui.screens.options.SoundOptionsScreen",
		"net.minecraft.client.gui.screens.options.LanguageSelectScreen",
		"net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen",
		"net.minecraft.client.gui.screens.options.FontOptionsScreen",
		"net.minecraft.client.gui.screens.options.OnlineOptionsScreen",
		"net.minecraft.client.gui.screens.options.ChatOptionsScreen",
		"net.minecraft.client.gui.screens.options.InWorldGameRulesScreen",
		"net.minecraft.client.gui.screens.packs.PackSelectionScreen",
		"net.minecraft.client.gui.screens.ShareToLanScreen",
		// Legacy4J
		"wily.legacy.client.screen.SettingsScreen",
		"wily.legacy.client.screen.Legacy4JSettingsScreen",
		"wily.legacy.client.screen.ModsScreen",
		"wily.legacy.client.screen.Legacy4JStoreScreen",
		"wily.legacy.client.screen.Legacy4JContentListScreen",
		"wily.legacy.client.screen.LegacyLanguageScreen",
		"wily.legacy.client.screen.PublishScreen",
		"wily.legacy.client.screen.GameHostOptionsScreen",
		"wily.legacy.client.screen.PlayerHostOptionsScreen",
		"wily.legacy.client.screen.compat.WorldHostFriendsScreen",
		"wily.legacy.client.screen.OptionsPresetScreen",
		// Couchcraft
		"dev.couchcraft.gui.CouchScreen"
	);

	/** Buttons hidden from guests in Legacy4J's pause and Help & Options menus. */
	private static final Set<String> HIDDEN_BUTTONS = Set.of(
		"legacy.menu.settings",
		"credits_and_attribution.button.credits",
		"legacy.menu.reinstall_content",
		"legacy.menu.save",
		"legacy.menu.disable_autosave"
	);

	private GuestScreens() {
	}

	public static boolean isBlocked(final Screen screen) {
		for (Class<?> c = screen.getClass(); c != null && c != Screen.class; c = c.getSuperclass()) {
			if (BLOCKED.contains(c.getName())) {
				return true;
			}
		}

		return false;
	}

	public static void pruneButtons(final Screen screen) {
		if (!(screen instanceof RenderableVList.Access access)) {
			return;
		}

		try {
			for (RenderableVList list : access.getRenderableVLists()) {
				list.renderables.removeIf(r -> r instanceof AbstractWidget widget
					&& widget.getMessage().getContents() instanceof TranslatableContents contents
					&& HIDDEN_BUTTONS.contains(contents.getKey()));
			}
		} catch (Throwable t) {
			CouchLegacyBridge.LOGGER.warn("[couchlegacy] Could not trim a menu for a guest", t);
		}
	}
}
