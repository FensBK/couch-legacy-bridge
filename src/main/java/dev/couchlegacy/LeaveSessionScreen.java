package dev.couchlegacy;

import dev.couchcraft.context.Couch;
import dev.couchcraft.context.PlayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.screen.ConfirmationScreen;

/** Legacy-styled confirmation shown when a guest picks "Exit game": it only removes that player. */
public final class LeaveSessionScreen {
	private LeaveSessionScreen() {
	}

	public static Screen create(final Screen parent, final PlayerContext guest) {
		Component name = Component.literal(guest.name);
		return new ConfirmationScreen(
			parent,
			Component.translatable("couchlegacy.leave.title"),
			Component.translatable("couchlegacy.leave.message", name),
			s -> leave(guest)
		) {
			@Override
			protected void addButtons() {
				this.renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose()).build());
				this.renderableVList.addRenderable(Button.builder(Component.translatable("couchlegacy.leave.button"), b -> leave(guest)).build());
			}
		};
	}

	private static void leave(final PlayerContext guest) {
		Minecraft.getInstance().gui.setScreen(null);
		Couch.requestRemove(guest);
	}
}
