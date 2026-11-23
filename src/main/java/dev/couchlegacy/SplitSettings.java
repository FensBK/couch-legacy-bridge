package dev.couchlegacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.couchcraft.CouchConfig;
import dev.couchcraft.context.Couch;
import dev.couchcraft.context.Layouts;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigControl;
import wily.factoryapi.base.config.FactoryConfigDisplay;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.OptionsScreen;

/**
 * Split-screen options inside Legacy4J's own settings menus, like the Nintendo Switch Edition:
 * - Game Options: "Vertical Split Screen" (Couchcraft's two-player layout: side by side).
 * - User Interface: "HUD Size (Split Screen)", used instead of "HUD Size" while the screen is split.
 */
public final class SplitSettings {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("couchlegacy").resolve("settings.json");

	private static final class Data {
		int splitHudSize = 2;
	}

	private static Data data;

	public static final FactoryConfig<Boolean> VERTICAL_SPLIT = FactoryConfig.create(
		"couchlegacy.verticalSplit",
		FactoryConfigDisplay.createToggle(Component.translatable("couchlegacy.options.vertical_split")),
		false,
		Bearer.of(
			() -> CouchConfig.get().twoPlayerLayout() == Layouts.TwoPlayer.SIDE_BY_SIDE,
			v -> {
				CouchConfig.get().twoPlayerLayout = (v ? Layouts.TwoPlayer.SIDE_BY_SIDE : Layouts.TwoPlayer.STACKED).name();
				// Couchcraft only re-lays out the viewports when told to.
				Couch.markLayoutDirty();
			}),
		FactoryConfigControl.TOGGLE,
		v -> {},
		CouchConfig::save);

	public static final FactoryConfig<Integer> SPLIT_HUD_SIZE = FactoryConfig.create(
		"couchlegacy.splitHudSize",
		FactoryConfigDisplay.intBuilder().build(Component.translatable("couchlegacy.options.split_hud_size")),
		2,
		Bearer.of(() -> data().splitHudSize, v -> data().splitHudSize = Math.max(1, Math.min(3, v))),
		new FactoryConfigControl.Int(1, () -> 3, Integer.MAX_VALUE),
		v -> {},
		SplitSettings::save);

	private SplitSettings() {
	}

	/** The HUD size Legacy4J should use right now. */
	public static int effectiveHudSize(final int normal) {
		return Couch.ready() && Couch.isSplit() ? data().splitHudSize : normal;
	}

	/** Called once Legacy4J's settings sections exist. */
	public static void addToLegacyMenus() {
		List<Consumer<OptionsScreen>> game = OptionsScreen.Section.GAME_OPTIONS.elements();
		game.add(0, o -> o.getRenderableVList().addOptions(VERTICAL_SPLIT));

		List<Consumer<OptionsScreen>> ui = OptionsScreen.Section.USER_INTERFACE.elements();
		ui.add(o -> {
			List<Renderable> renderables = o.getRenderableVList().renderables;
			String hudName = LegacyOptions.hudSize.getDisplay().name().getString();
			int index = -1;
			for (int i = 0; i < renderables.size(); i++) {
				if (renderables.get(i) instanceof AbstractWidget widget && widget.getMessage().getString().startsWith(hudName)) {
					index = i + 1;
				}
			}

			if (index >= 0) {
				o.getRenderableVList().addOptions(index, SPLIT_HUD_SIZE);
			} else {
				o.getRenderableVList().addOptions(SPLIT_HUD_SIZE);
			}
		});
	}

	private static Data data() {
		if (data == null) {
			data = new Data();
			try {
				if (Files.exists(FILE)) {
					try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
						Data loaded = GSON.fromJson(reader, Data.class);
						if (loaded != null) {
							data = loaded;
						}
					}
				}
			} catch (Exception e) {
				CouchLegacyBridge.LOGGER.warn("[couchlegacy] Could not read {}", FILE, e);
			}
		}

		return data;
	}

	private static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(data(), writer);
			}
		} catch (Exception e) {
			CouchLegacyBridge.LOGGER.warn("[couchlegacy] Could not save {}", FILE, e);
		}
	}
}
