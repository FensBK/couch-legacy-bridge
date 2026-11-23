package dev.couchlegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.couchcraft.context.Couch;
import dev.couchcraft.input.Gamepad;
import dev.couchlegacy.CouchLegacyBridge;
import dev.couchlegacy.JoinPrompt;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Couch.class, remap = false)
public abstract class CouchMixin {
	/** Runs once per frame, after Couchcraft's own frame start. */
	@Inject(method = "frameStart", at = @At("RETURN"))
	private static void couchlegacy$afterFrameStart(final Minecraft minecraft, final CallbackInfo ci) {
		CouchLegacyBridge.afterFrameStart(minecraft);
	}

	/**
	 * Joining works like the console editions: START once shows a prompt, START again joins.
	 * BACK on another controller no longer takes over player 1: player 1's controller is Legacy4J's.
	 */
	@WrapOperation(method = "handleControllerJoins", at = @At(value = "INVOKE", target = "Ldev/couchcraft/input/Gamepad;pressed(I)Z"))
	private static boolean couchlegacy$consoleStyleJoin(final Gamepad pad, final int button, final Operation<Boolean> original) {
		boolean pressed = original.call(pad, button);
		if (button == JoinPrompt.SDL_BACK) {
			return false;
		}

		if (button == JoinPrompt.SDL_START && pressed) {
			return JoinPrompt.confirm(pad);
		}

		return pressed;
	}
}
