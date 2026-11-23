package dev.couchlegacy.mixin;

import dev.couchcraft.context.PlayerContext;
import dev.couchcraft.input.Gamepad;
import dev.couchcraft.input.PadController;
import dev.couchlegacy.GuestPadBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every controller belongs to Legacy4J.
 *
 * Player 1: Couchcraft keeps the pad assigned (so START on it never spawns a guest) but stops acting on it.
 * Guests: Couchcraft's mouse-cursor controls are replaced by Legacy4J's own controller update, run on the
 * guest's pad inside the guest's scope. Couchcraft's hold BACK+START to leave still works.
 */
@Mixin(value = PadController.class, remap = false)
public abstract class PadControllerMixin {
	@Shadow
	@Final
	private PlayerContext context;

	@Shadow
	private boolean checkLeaveCombo(final Gamepad pad, final long now) {
		throw new AssertionError();
	}

	@Inject(method = "frame", at = @At("HEAD"), cancellable = true)
	private void couchlegacy$legacyOwnsEveryPad(final Minecraft minecraft, final CallbackInfo ci) {
		if (this.context.primary) {
			ci.cancel();
			return;
		}

		if (GuestPadBridge.isBroken()) {
			return;
		}

		ci.cancel();
		Gamepad pad = this.context.pad;
		if (pad != null && this.checkLeaveCombo(pad, Util.getNanos())) {
			return;
		}

		GuestPadBridge.drive(this.context);
	}
}
