package dev.couchlegacy.mixin;

import dev.couchcraft.context.Couch;
import dev.couchcraft.context.PlayerContext;
import dev.couchlegacy.CouchLegacyBridge;
import dev.couchlegacy.GuestSkinStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.skins.skin.SkinDataStore;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Legacy Skins stores one selected skin and one favorites list for the whole game. While a guest
 * is live, read and write that guest's own entry instead, so guests never overwrite player 1's
 * saved skin or favorites, and each profile keeps its own.
 */
@Mixin(value = SkinDataStore.class, remap = false)
public abstract class SkinDataStoreMixin {
	@Inject(method = "setSelectedSkin(Ljava/util/UUID;Ljava/lang/String;Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
	private static void couchlegacy$setSelected(final UUID userId, final String skinId, final String packId, final CallbackInfo ci) {
		UUID guest = CouchLegacyBridge.activeGuestSkinId();
		PlayerContext current = Couch.ready() ? Couch.current() : null;
		CouchLegacyBridge.LOGGER.info("[CouchLegacyBridge] Skin saved for {} (live player: {}, store: {}): {}",
			userId, current == null ? "none" : current.name, guest != null ? "guest profile" : "Legacy4J", skinId);
		if (guest == null) {
			return;
		}

		GuestSkinStore.Entry entry = GuestSkinStore.get(guest);
		entry.skin = SkinIdUtil.normalize(skinId);
		String pack = SkinIdUtil.trimToNull(packId);
		entry.pack = pack == null ? "" : pack;
		GuestSkinStore.save();
		ci.cancel();
	}

	@Inject(method = "getSelectedSkin", at = @At("HEAD"), cancellable = true)
	private static void couchlegacy$getSelected(final UUID userId, final CallbackInfoReturnable<String> cir) {
		UUID guest = CouchLegacyBridge.activeGuestSkinId();
		if (guest != null && guest.equals(userId)) {
			cir.setReturnValue(SkinIdUtil.normalize(GuestSkinStore.get(guest).skin));
		}
	}

	@Inject(method = "getSelectedPack", at = @At("HEAD"), cancellable = true)
	private static void couchlegacy$getPack(final UUID userId, final CallbackInfoReturnable<String> cir) {
		UUID guest = CouchLegacyBridge.activeGuestSkinId();
		if (guest != null && guest.equals(userId)) {
			cir.setReturnValue(SkinIdUtil.trimToNull(GuestSkinStore.get(guest).pack));
		}
	}

	@Inject(method = "isFavorite", at = @At("HEAD"), cancellable = true)
	private static void couchlegacy$isFavorite(final String skinId, final CallbackInfoReturnable<Boolean> cir) {
		UUID guest = CouchLegacyBridge.activeGuestSkinId();
		if (guest != null) {
			cir.setReturnValue(couchlegacy$guestFavorites(guest).contains(SkinIdUtil.normalize(skinId)));
		}
	}

	@Inject(method = "getFavorites", at = @At("HEAD"), cancellable = true)
	private static void couchlegacy$getFavorites(final CallbackInfoReturnable<List<String>> cir) {
		UUID guest = CouchLegacyBridge.activeGuestSkinId();
		if (guest != null) {
			cir.setReturnValue(List.copyOf(couchlegacy$guestFavorites(guest)));
		}
	}

	@Inject(method = "toggleFavorite", at = @At("HEAD"), cancellable = true)
	private static void couchlegacy$toggleFavorite(final String skinId, final CallbackInfo ci) {
		UUID guest = CouchLegacyBridge.activeGuestSkinId();
		if (guest == null) {
			return;
		}

		ci.cancel();
		String id = SkinIdUtil.normalize(skinId);
		if (!SkinIdUtil.hasSkin(id)) {
			return;
		}

		LinkedHashSet<String> ids = couchlegacy$guestFavorites(guest);
		if (!ids.remove(id)) {
			ids.add(id);
		}

		GuestSkinStore.get(guest).favorites = List.copyOf(ids);
		GuestSkinStore.save();
	}

	private static LinkedHashSet<String> couchlegacy$guestFavorites(final UUID guest) {
		LinkedHashSet<String> ids = new LinkedHashSet<>();
		for (String value : GuestSkinStore.get(guest).favorites) {
			String id = SkinIdUtil.normalize(value);
			if (SkinIdUtil.hasSkin(id)) {
				ids.add(id);
			}
		}

		return ids;
	}
}
