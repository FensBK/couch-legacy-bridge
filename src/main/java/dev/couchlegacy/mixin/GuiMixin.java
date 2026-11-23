package dev.couchlegacy.mixin;

import dev.couchcraft.context.PlayerContext;
import dev.couchlegacy.CouchLegacyBridge;
import dev.couchlegacy.GuestScreens;
import dev.couchlegacy.LeaveSessionScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.ExitConfirmationScreen;

/**
 * Guest menus, console style:
 * - "Exit game" only takes the guest out of the split-screen session.
 * - Game-wide settings (graphics, audio, mods, host options, saving) are player 1's only.
 */
@Mixin(value = Gui.class, remap = false)
public abstract class GuiMixin {
	@Shadow
	public abstract Screen screen();

	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void couchlegacy$guestCannotOpenHostScreens(final Screen screen, final CallbackInfo ci) {
		if (screen != null && CouchLegacyBridge.activeGuest() != null && GuestScreens.isBlocked(screen)) {
			ci.cancel();
		}
	}

	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen couchlegacy$guestMenus(final Screen screen) {
		PlayerContext guest = CouchLegacyBridge.activeGuest();
		if (guest == null || screen == null) {
			return screen;
		}

		if (screen instanceof ExitConfirmationScreen) {
			return LeaveSessionScreen.create(this.screen(), guest);
		}

		GuestScreens.pruneButtons(screen);
		return screen;
	}
}
