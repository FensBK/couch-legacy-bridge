package dev.couchlegacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Skin selection and favorites for Couchcraft guest profiles.
 *
 * Legacy Skins keeps one selected skin (tagged with one owner) and one favorites list for the whole
 * game, in its own options file. The primary player keeps using that. Each guest profile gets its own
 * entry here, keyed by the Couchcraft profile id, saved in config/couchlegacy/guest_skins.json.
 */
public final class GuestSkinStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("couchlegacy").resolve("guest_skins.json");

	public static final class Entry {
		public String skin = "";
		public String pack = "";
		public List<String> favorites = new ArrayList<>();
	}

	private static Map<String, Entry> entries;

	private GuestSkinStore() {
	}

	private static synchronized Map<String, Entry> entries() {
		if (entries == null) {
			entries = new LinkedHashMap<>();
			if (Files.isRegularFile(FILE)) {
				try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
					Map<String, Entry> loaded = GSON.fromJson(reader, new TypeToken<LinkedHashMap<String, Entry>>() {}.getType());
					if (loaded != null) {
						entries.putAll(loaded);
					}
				} catch (IOException | RuntimeException e) {
					CouchLegacyBridge.LOGGER.warn("[CouchLegacyBridge] Could not read {}", FILE, e);
				}
			}
		}

		return entries;
	}

	public static synchronized Entry get(final UUID profile) {
		return entries().computeIfAbsent(profile.toString(), k -> new Entry());
	}

	public static synchronized void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(entries(), writer);
			}
		} catch (IOException | RuntimeException e) {
			CouchLegacyBridge.LOGGER.warn("[CouchLegacyBridge] Could not save {}", FILE, e);
		}
	}
}
