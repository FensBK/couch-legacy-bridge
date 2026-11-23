package dev.couchlegacy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.skins.client.screen.AbstractChangeSkinScreen;
import wily.legacy.skins.skin.SkinPackLoader;

/**
 * Legacy Skins builds a single "Favorites" pack for the whole game. Rebuild it whenever a player
 * opens the skin screen, so it lists that player's favorites (the screen opens inside their scope).
 */
@Mixin(value = AbstractChangeSkinScreen.class, remap = false)
public abstract class AbstractChangeSkinScreenMixin {
	@Inject(method = "init", at = @At("HEAD"))
	private void couchlegacy$favoritesOfThisPlayer(final CallbackInfo ci) {
		SkinPackLoader.rebuildFavouritesPack();
	}
}
