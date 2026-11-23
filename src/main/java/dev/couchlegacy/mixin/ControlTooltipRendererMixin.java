package dev.couchlegacy.mixin;

import dev.couchlegacy.CouchLegacyBridge;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.control.tooltip.ControlTooltipRenderer;

import java.util.Map;

/**
 * Legacy4J draws the button hints (bottom-left bar) for the HUD and for every screen through one
 * shared {@link ControlTooltipRenderer}. With two players the HUD of one and the menu of the other
 * keep rewriting the same list, so the bar flickers and shows up in the wrong viewport.
 */
@Mixin(value = ControlTooltipRenderer.class, remap = false)
public abstract class ControlTooltipRendererMixin {
	@Shadow
	@Final
	protected Map<?, ?> renderTooltips;

	/** Each guest gets its own bar; the primary player keeps Legacy4J's instance. */
	@Inject(method = "getInstance", at = @At("HEAD"), cancellable = true)
	private static void couchlegacy$perPlayerRenderer(final CallbackInfoReturnable<ControlTooltipRenderer> cir) {
		ControlTooltipRenderer guest = CouchLegacyBridge.guestTooltipRenderer();
		if (guest != null) {
			cir.setReturnValue(guest);
		}
	}

	/**
	 * While a guest is on Couchcraft's "Who's playing?" screen there is a world but no player for
	 * them yet. Legacy4J's hints assume a player in that case (JUMP calls player.isUnderWater())
	 * and crash, so draw nothing until the guest has spawned.
	 */
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void couchlegacy$skipWithoutPlayer(final CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null && minecraft.player == null) {
			this.renderTooltips.clear();
			ci.cancel();
		}
	}
}
