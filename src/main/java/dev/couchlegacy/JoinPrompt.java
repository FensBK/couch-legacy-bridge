package dev.couchlegacy;

import dev.couchcraft.context.Couch;
import dev.couchcraft.input.Gamepad;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import wily.legacy.util.client.LegacyRenderUtil;

/**
 * Console-style joining: START on a free controller shows a prompt in the middle of the screen,
 * and pressing START again on that same controller opens the profile picker.
 */
public final class JoinPrompt {
	/** SDL3 gamepad button indices (SDL_GAMEPAD_BUTTON_BACK / _START). */
	public static final int SDL_BACK = 4;
	public static final int SDL_START = 6;
	private static final long WINDOW_MS = 6000L;

	private static WeakReference<Gamepad> pending = new WeakReference<>(null);
	private static long deadline;

	private JoinPrompt() {
	}

	/** Called for START on a free controller. True means "join now". */
	public static boolean confirm(final Gamepad pad) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null || !Couch.canAddGuest(minecraft)) {
			pending.clear();
			return false;
		}

		long now = Util.getMillis();
		if (pending.get() == pad && now < deadline) {
			pending.clear();
			return true;
		}

		pending = new WeakReference<>(pad);
		deadline = now + WINDOW_MS;
		pad.rumble(0.25F, 0.25F, 120);
		return false;
	}

	public static void render(final GuiGraphicsExtractor graphics) {
		Gamepad pad = pending.get();
		if (pad == null) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (Util.getMillis() >= deadline || !pad.isConnected() || pad.owner() != null || minecraft.level == null || !Couch.canAddGuest(minecraft)) {
			pending.clear();
			return;
		}

		Component title = Component.translatable("couchlegacy.join.title");
		Component line = Component.translatable("couchlegacy.join.message");
		int textWidth = Math.max(minecraft.font.width(title), minecraft.font.width(line));
		int width = textWidth + 32;
		int height = 46;
		int x = (graphics.guiWidth() - width) / 2;
		int y = (graphics.guiHeight() - height) / 2;
		graphics.nextStratum();
		LegacyRenderUtil.renderPointerPanel(graphics, x, y, width, height);
		graphics.text(minecraft.font, title, x + (width - minecraft.font.width(title)) / 2, y + 10, ARGB.color(255, 0xFFFFFF), true);
		float pulse = 0.6F + 0.4F * (float)Math.sin(System.currentTimeMillis() / 300.0);
		graphics.text(minecraft.font, line, x + (width - minecraft.font.width(line)) / 2, y + 27, ARGB.color(Math.round(pulse * 255.0F), 0xFFFFFF), true);
	}
}
