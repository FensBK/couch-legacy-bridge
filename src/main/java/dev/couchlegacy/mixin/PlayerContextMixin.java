package dev.couchlegacy.mixin;

import dev.couchcraft.context.PlayerContext;
import dev.couchlegacy.CouchLegacyBridge;
import dev.couchlegacy.GuestPadBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into Couchcraft's per-player state swap.
 *
 * - On creation, a guest's HUD gets Legacy4J's HUD layout.
 * - On every swap, Legacy4J's HUD timers and its controller state (ControllerManager flags and every
 *   button's state) are saved and restored per player.
 */
@Mixin(value = PlayerContext.class, remap = false)
public abstract class PlayerContextMixin {
	@Inject(method = "<init>", at = @At("TAIL"))
	private void couchlegacy$legacyHudForGuests(final CallbackInfo ci) {
		CouchLegacyBridge.onContextCreated((PlayerContext)(Object)this);
	}

	@Inject(method = "saveFrom", at = @At("TAIL"))
	private void couchlegacy$saveLegacyState(final CallbackInfo ci) {
		CouchLegacyBridge.saveHudState((PlayerContext)(Object)this);
		GuestPadBridge.save((PlayerContext)(Object)this);
	}

	@Inject(method = "loadInto", at = @At("TAIL"))
	private void couchlegacy$loadLegacyState(final CallbackInfo ci) {
		CouchLegacyBridge.loadHudState((PlayerContext)(Object)this);
		GuestPadBridge.load((PlayerContext)(Object)this);
	}
}
