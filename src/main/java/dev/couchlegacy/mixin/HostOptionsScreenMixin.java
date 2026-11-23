package dev.couchlegacy.mixin;

import dev.couchlegacy.CouchLegacyBridge;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.HostOptionsScreen;

/**
 * Guests open the player list (SELECT) to see who is playing and to open the chat, like on console.
 * The "Host Options" button (game rules, time, weather, player privileges) is player 1's only.
 */
@Mixin(value = HostOptionsScreen.class, remap = false)
public abstract class HostOptionsScreenMixin {
	@Shadow
	protected Button hostOptionsButton;

	@Inject(method = "refreshHostOptionsButton", at = @At("TAIL"))
	private void couchlegacy$hostOptionsArePlayerOnes(final CallbackInfo ci) {
		if (this.hostOptionsButton != null && CouchLegacyBridge.activeGuest() != null) {
			this.hostOptionsButton.active = false;
			this.hostOptionsButton.visible = false;
		}
	}
}
