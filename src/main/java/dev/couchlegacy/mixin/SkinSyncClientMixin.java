package dev.couchlegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.couchlegacy.CouchLegacyBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.skins.skin.SkinDataStore;
import wily.legacy.skins.skin.SkinIdUtil;
import wily.legacy.skins.skin.SkinSyncClient;

import java.util.UUID;

/**
 * Legacy Skins stores "my selected skin" under the logged-in account ({@code client.getUser()}).
 * Couchcraft never swaps the account, so a guest picking a skin also overwrote player 1's skin,
 * both on screen and in the saved selection. While a guest is live, use the guest's profile instead.
 */
@Mixin(value = SkinSyncClient.class, remap = false)
public abstract class SkinSyncClientMixin {
	/** Used for the saved selection (SkinDataStore) and for resolving the skin to send to the server. */
	@Inject(method = "getUserId", at = @At("HEAD"), cancellable = true)
	private static void couchlegacy$guestUserId(final Minecraft client, final CallbackInfoReturnable<UUID> cir) {
		UUID guest = CouchLegacyBridge.activeGuestSkinId();
		if (guest != null) {
			cir.setReturnValue(guest);
		}
	}

	/** The second cache write in cacheLocalSelection: "account UUID -> skin". This is what changed player 1. */
	@WrapOperation(
		method = "cacheLocalSelection",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/User;getProfileId()Ljava/util/UUID;")
	)
	private static UUID couchlegacy$guestCacheKey(final User user, final Operation<UUID> original) {
		UUID guest = CouchLegacyBridge.activeGuestSkinId();
		return guest != null ? guest : original.call(user);
	}

	/** Leaving as a guest must not wipe everyone's skin cache; only the primary player leaving the world should. */
	@Inject(method = "onClientDisconnect", at = @At("HEAD"), cancellable = true)
	private static void couchlegacy$keepCacheWhenGuestLeaves(final CallbackInfo ci) {
		if (CouchLegacyBridge.activeGuest() != null) {
			ci.cancel();
		}
	}

	/** Diagnostics: what the skin screen (or anything else) asks to apply, and in whose turn. */
	@Inject(method = "requestSetSkin(Lnet/minecraft/client/Minecraft;Ljava/lang/String;Ljava/lang/String;)V", at = @At("HEAD"))
	private static void couchlegacy$logRequest(final Minecraft client, final String packId, final String skinId, final CallbackInfo ci) {
		CouchLegacyBridge.LOGGER.info("[CouchLegacyBridge] Skin requested by {}: pack={} skin={}", CouchLegacyBridge.liveName(), packId, skinId);
	}

	/**
	 * Player 1 must only ever send player 1's own saved skin. Legacy Skins also sends from shared
	 * in-memory state (caches, the join handler), and a guest's skin leaked into player 1 that way.
	 * If player 1 is about to send a skin other than the one saved for their account, send the saved one.
	 */
	@ModifyVariable(method = "sendSelection(Lnet/minecraft/client/Minecraft;Ljava/lang/String;Z)V", at = @At("HEAD"), argsOnly = true)
	private static String couchlegacy$guardPrimarySkin(final String skinId) {
		String live = CouchLegacyBridge.liveName();
		if (CouchLegacyBridge.activeGuest() == null) {
			Minecraft client = Minecraft.getInstance();
			User user = client.getUser();
			if (user != null) {
				String saved = SkinDataStore.getSelectedSkin(user.getProfileId());
				String asked = SkinIdUtil.normalize(skinId);
				if (SkinIdUtil.hasSkin(saved) && !saved.equals(asked)) {
					CouchLegacyBridge.LOGGER.warn("[CouchLegacyBridge] Player 1 was about to send skin '{}' but their saved skin is '{}'; sending the saved one", asked, saved, new Throwable("where it came from"));
					return saved;
				}
			}
		}

		CouchLegacyBridge.LOGGER.info("[CouchLegacyBridge] Skin sent by {}: {}", live, skinId);
		return skinId;
	}
}
