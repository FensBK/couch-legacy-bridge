package dev.couchlegacy.mixin;

import dev.couchlegacy.CouchLegacyBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.control.ControllerManager;

/**
 * A guest's Legacy4J cursor is virtual: it moves only that player's pointer, never the real mouse
 * (which belongs to player 1), and never hides or shows the system cursor.
 */
@Mixin(value = ControllerManager.class, remap = false)
public abstract class ControllerManagerMixin {
	@ModifyVariable(method = "setRawPointerPos(DDZ)V", at = @At("HEAD"), argsOnly = true)
	private boolean couchlegacy$guestCursorIsVirtual(final boolean onlyVirtual) {
		return onlyVirtual || CouchLegacyBridge.activeGuest() != null;
	}

	@Inject(method = "setCursorInputMode", at = @At("HEAD"), cancellable = true)
	private void couchlegacy$guestLeavesSystemCursorAlone(final boolean hidden, final CallbackInfo ci) {
		if (CouchLegacyBridge.activeGuest() != null) {
			ci.cancel();
		}
	}
}
