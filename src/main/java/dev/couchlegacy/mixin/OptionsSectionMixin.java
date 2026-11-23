package dev.couchlegacy.mixin;

import dev.couchlegacy.SplitSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.OptionsScreen;

/** Adds the split-screen options once Legacy4J has built its settings sections. */
@Mixin(value = OptionsScreen.Section.class, remap = false)
public abstract class OptionsSectionMixin {
	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void couchlegacy$addSplitScreenOptions(final CallbackInfo ci) {
		SplitSettings.addToLegacyMenus();
	}
}
