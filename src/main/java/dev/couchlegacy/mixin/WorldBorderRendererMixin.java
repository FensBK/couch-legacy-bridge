package dev.couchlegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Every WorldBorderRenderer registers a brand-new forcefield texture when it is created. Couchcraft
 * creates one more renderer for each guest, which replaced the loaded texture with an empty one, and
 * the game crashed the next time a world border was on screen (e.g. the Tutorial world).
 * Only the first registration is kept; that texture is reloaded with the resource packs as usual.
 */
@Mixin(value = WorldBorderRenderer.class, remap = false)
public abstract class WorldBorderRendererMixin {
	@Unique
	private static boolean couchlegacy$forcefieldRegistered;

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;register(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/renderer/texture/AbstractTexture;)V"))
	private void couchlegacy$registerForcefieldOnce(final TextureManager manager, final Identifier id, final AbstractTexture texture, final Operation<Void> original) {
		if (couchlegacy$forcefieldRegistered) {
			return;
		}

		couchlegacy$forcefieldRegistered = true;
		original.call(manager, id, texture);
	}
}
