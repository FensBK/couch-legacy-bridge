package dev.couchlegacy;

import dev.couchcraft.context.Couch;
import dev.couchcraft.context.PlayerContext;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.control.tooltip.ControlTooltip;
import wily.legacy.client.control.tooltip.ControlTooltipRenderer;
import wily.legacy.util.client.LegacyGuiElements;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Shared helpers for the mixins.
 *
 * Couchcraft keeps one {@link PlayerContext} per player and swaps it onto the Minecraft singleton
 * ({@code Couch.enter}). Legacy4J keeps some per-player state in its own singletons and statics,
 * which Couchcraft does not know about. Everything here routes that state per player.
 */
public final class CouchLegacyBridge implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("CouchLegacyBridge");

	/** One Legacy4J control tooltip bar per guest. The primary player keeps Legacy4J's own instance. */
	private static final Map<PlayerContext, ControlTooltipRenderer> GUEST_TOOLTIPS = new WeakHashMap<>();
	/** Legacy4J's HUD timers per player: {lastHotbarSelectionChange, lastHotbarSelection, lastGui, dollTime, dollRemaining, canSprint, sprintTicksLeft}. */
	private static final Map<PlayerContext, long[]> HUD_STATE = new WeakHashMap<>();

	private static boolean wasSplit;
	private static int framesUntilResize = -1;

	@Override
	public void onInitializeClient() {
		wily.factoryapi.FactoryAPIClient.registerDefaultConfigScreen("couchcraft", dev.couchcraft.gui.CouchScreen::new);
		wily.factoryapi.FactoryAPIClient.registerDefaultConfigScreen("couchlegacy", dev.couchcraft.gui.CouchScreen::new);
		LOGGER.info("[CouchLegacyBridge] Loaded v0.7.5: Portuguese for Couchcraft, guests get the player list and chat (no host controls), no name tags on GUI player figures, split-screen options in Legacy settings (vertical split, split HUD size), per-player double-tap sprint, world border crash fix, guest menus limited to their own settings, console-style join, per-player paper doll and cursor, split-screen settings in the Mods menu, Legacy4J runs every player's controller, guest leave-session dialog, Legacy HUD for guests, per-profile skins and favorites, per-player tooltips and HUD timers.");
	}

	/** The guest whose state is live on the singleton, or null when it is the primary player (or no split screen). */
	public static @Nullable PlayerContext activeGuest() {
		if (!Couch.ready() || !Couch.isGuestActive()) {
			return null;
		}

		return Couch.current();
	}

	/** Name of the player whose state is live right now, for logs. */
	public static String liveName() {
		if (!Couch.ready()) {
			return "no split screen";
		}

		PlayerContext current = Couch.current();
		return current == null ? "player 1 (no scope)" : current.name;
	}

	// ---------------------------------------------------------------- control tooltips

	/**
	 * Tooltip renderer for the live guest, or null to keep Legacy4J's default (primary player).
	 * Created lazily the first time the guest's HUD or a guest screen asks for it.
	 */
	public static @Nullable ControlTooltipRenderer guestTooltipRenderer() {
		PlayerContext guest = activeGuest();
		if (guest == null) {
			return null;
		}

		ControlTooltipRenderer renderer = GUEST_TOOLTIPS.get(guest);
		if (renderer == null) {
			renderer = new ControlTooltipRenderer();
			// Store before setup: setup asks getInstance() again and must find this instance.
			GUEST_TOOLTIPS.put(guest, renderer);
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.gui.screen() == null && minecraft.player != null) {
				ControlTooltip.Listener.of(minecraft.gui).setupControlTooltips();
			}
		}

		return renderer;
	}

	// ---------------------------------------------------------------- skins

	/**
	 * Identity Legacy Skins should store and cache a skin selection under. Legacy Skins uses the
	 * logged-in account, which is always the primary player's; for a guest we use the guest's
	 * Couchcraft profile, whose id is saved with the profile, so the choice survives restarts.
	 */
	public static @Nullable UUID activeGuestSkinId() {
		PlayerContext guest = activeGuest();
		if (guest == null) {
			return null;
		}

		if (guest.profileId != null) {
			return guest.profileId;
		}

		LocalPlayer player = guest.player();
		return player == null ? null : player.getUUID();
	}

	// ---------------------------------------------------------------- HUD

	/** Gives a new guest's HUD the Legacy4J layout that Legacy4J installed on the startup HUD only. */
	public static void onContextCreated(final PlayerContext context) {
		if (context.primary || !Couch.ready()) {
			return;
		}

		try {
			UIAccessor primaryHud = UIAccessor.of(Couch.primary().gui.hud);
			UIAccessor guestHud = UIAccessor.of(context.gui.hud);
			for (UIDefinition definition : primaryHud.getStaticDefinitions()) {
				if (!guestHud.getStaticDefinitions().contains(definition)) {
					guestHud.addStatic(definition);
				}
			}

			guestHud.getElements().putAll(primaryHud.getElements());
		} catch (RuntimeException e) {
			LOGGER.warn("[CouchLegacyBridge] Could not apply the Legacy4J HUD to {}", context.name, e);
		}
	}

	public static void saveHudState(final PlayerContext context) {
		HUD_STATE.put(context, new long[]{
			LegacyGuiElements.lastHotbarSelectionChange,
			LegacyGuiElements.lastHotbarSelection,
			LegacyGuiElements.lastGui,
			wily.legacy.client.AnimatedCharacterRenderer.time,
			wily.legacy.client.AnimatedCharacterRenderer.remainingTime,
			wily.legacy.Legacy4JClient.canSprint ? 1L : 0L,
			wily.legacy.Legacy4JClient.sprintTicksLeft
		});
	}

	public static void loadHudState(final PlayerContext context) {
		long[] state = HUD_STATE.get(context);
		if (state == null) {
			if (context.primary) {
				return;
			}

			state = new long[]{-1L, -1L, -1L, 0L, 0L, 0L, -1L};
		}

		LegacyGuiElements.lastHotbarSelectionChange = state[0];
		LegacyGuiElements.lastHotbarSelection = (int)state[1];
		LegacyGuiElements.lastGui = state[2];
		// The paper doll (top-left player figure shown while sprinting, sneaking, flying...) has its own timer.
		wily.legacy.client.AnimatedCharacterRenderer.time = state[3];
		wily.legacy.client.AnimatedCharacterRenderer.remainingTime = state[4];
		// Double-tap-forward sprint detection: shared, player 1's menu kept resetting it for everyone.
		wily.legacy.Legacy4JClient.canSprint = state[5] != 0L;
		wily.legacy.Legacy4JClient.sprintTicksLeft = (int)state[6];
	}

	// ---------------------------------------------------------------- leaving split screen

	/**
	 * When the last guest leaves (usually because the world was closed), the title screen could come
	 * back laid out for half a window. Re-measure the GUI a few frames later, once the next screen is up.
	 */
	public static void afterFrameStart(final Minecraft minecraft) {
		boolean split = Couch.ready() && Couch.isSplit();
		if (wasSplit && !split) {
			framesUntilResize = 3;
		}

		wasSplit = split;
		if (framesUntilResize > 0 && --framesUntilResize == 0) {
			framesUntilResize = -1;
			minecraft.resizeGui();
		}
	}
}
