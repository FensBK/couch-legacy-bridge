package dev.couchlegacy.mixin;

import dev.couchcraft.context.Couch;
import dev.couchcraft.context.PlayerContext;
import dev.couchcraft.render.CouchHud;
import dev.couchlegacy.JoinPrompt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CouchHud.class, remap = false)
public abstract class CouchHudMixin {
	/** No "START to join · BACK to play as P1" corner text. */
	@Inject(method = "drawHint", at = @At("HEAD"), cancellable = true)
	private static void couchlegacy$noJoinHint(final GuiGraphicsExtractor graphics, final Minecraft minecraft, final Component hint, final CallbackInfo ci) {
		ci.cancel();
	}

	/** The "press START again" prompt, in the middle of player 1's view. */
	@Inject(method = "extract", at = @At("TAIL"))
	private static void couchlegacy$joinPrompt(final GuiGraphicsExtractor graphics, final CallbackInfo ci) {
		PlayerContext context = Couch.ready() ? Couch.current() : null;
		if (context != null && context.primary) {
			JoinPrompt.render(graphics);
		}
	}
}
