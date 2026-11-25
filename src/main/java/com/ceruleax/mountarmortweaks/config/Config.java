package com.ceruleax.mountarmortweaks.config;

import com.ceruleax.mountarmortweaks.MountArmorTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

/**
 * The two config files in {@code config/mountarmortweaks/}.
 *
 * <p>Settings fall into two groups:
 * <ul>
 *   <li>{@link Startup}: item components and villager trades. Read once per server start and kept
 *       until the server stops. Item components are copied into every item stack when it is created,
 *       so changing them on {@code /reload} would leave old and new stacks behaving differently;
 *       villager trades live in a world registry, which {@code /reload} does not touch.</li>
 *   <li>{@link Live}: recipes, the anvil switch and the enchantment whitelists. Re-read on every
 *       {@code /reload}.</li>
 * </ul>
 *
 * <p>A missing file is written with the defaults. A file that cannot be parsed, or a value of the
 * wrong type, is logged and replaced by its default; the server never fails to start because of it.
 * Values are read from worker threads during data-pack loading, hence the volatile fields.
 */
public final class Config {
    public static final String CONFIG_FILE = "config.json";
    public static final String ENCHANTMENTS_FILE = "enchantments.json";

    /** The eight craftable items, in file order. Netherite already has a vanilla smithing recipe. */
    public static final List<String> RECIPE_ITEMS = List.of(
        "copper_horse_armor", "iron_horse_armor", "golden_horse_armor", "diamond_horse_armor",
        "copper_nautilus_armor", "iron_nautilus_armor", "golden_nautilus_armor", "diamond_nautilus_armor"
    );

    public enum TradesMode {
        OFF, HORSE, NAUTILUS, BOTH;

        public boolean horse() {
            return this == HORSE || this == BOTH;
        }

        public boolean nautilus() {
            return this == NAUTILUS || this == BOTH;
        }
    }

    /** Settings that only change on a server restart. */
    public record Startup(boolean enchantingTable, boolean durability, TradesMode trades) {
    }

    /** Settings that follow {@code /reload}. */
    public record Live(Map<String, Boolean> recipes, boolean anvil, Whitelist whitelist) {
        public boolean recipe(String item) {
            return recipes.getOrDefault(item, true);
        }
    }

    /** Raw whitelist entries: enchantment ids or {@code #tag} ids, not yet resolved. */
    public record Whitelist(List<String> horseArmor, List<String> nautilusArmor, List<String> withDurability) {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static volatile @Nullable Startup startup;
    private static volatile @Nullable Live live;

    private Config() {
    }

    public static Startup startup() {
        Startup s = startup;
        if (s == null) {
            load();
            s = startup;
        }
        return s;
    }

    public static Live live() {
        Live l = live;
        if (l == null) {
            load();
            l = live;
        }
        return l;
    }

    /** Re-reads both files before a {@code /reload}. Startup settings are kept; changes are logged. */
    public static synchronized void reloadLive() {
        Startup kept = startup;
        Loaded loaded = read();
        live = loaded.live();
        if (kept == null) {
            startup = loaded.startup();
        } else if (!kept.equals(loaded.startup())) {
            MountArmorTweaks.LOGGER.info(
                "enchanting.table, durability and trades.mode changes in {} take effect after a server restart",
                CONFIG_FILE
            );
        }
    }

    /** Forgets everything when a server stops, so that the next world (or restart) reads fresh values. */
    public static synchronized void reset() {
        startup = null;
        live = null;
    }

    private static synchronized void load() {
        if (startup != null && live != null) {
            return;
        }
        Loaded loaded = read();
        if (startup == null) {
            startup = loaded.startup();
        }
        if (live == null) {
            live = loaded.live();
        }
    }

    private record Loaded(Startup startup, Live live) {
    }

    private static Loaded read() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve(MountArmorTweaks.MOD_ID);
        JsonObject config = readOrCreate(dir.resolve(CONFIG_FILE), defaultConfig());
        JsonObject enchantments = readOrCreate(dir.resolve(ENCHANTMENTS_FILE), defaultEnchantments());

        Reader r = new Reader(CONFIG_FILE);
        JsonObject recipesJson = r.object(config, "recipes");
        Map<String, Boolean> recipes = new LinkedHashMap<>();
        for (String item : RECIPE_ITEMS) {
            recipes.put(item, r.bool(recipesJson, "recipes." + item, item, true));
        }
        JsonObject enchanting = r.object(config, "enchanting");
        boolean table = r.bool(enchanting, "enchanting.table", "table", true);
        boolean anvil = r.bool(enchanting, "enchanting.anvil", "anvil", true);
        boolean durability = r.bool(config, "durability", "durability", false);
        JsonObject trades = r.object(config, "trades");
        TradesMode mode = r.tradesMode(trades);

        Reader e = new Reader(ENCHANTMENTS_FILE);
        Whitelist whitelist = new Whitelist(
            e.strings(enchantments, "horse_armor", DEFAULT_HORSE),
            e.strings(enchantments, "nautilus_armor", DEFAULT_NAUTILUS),
            e.strings(enchantments, "with_durability", DEFAULT_WITH_DURABILITY)
        );

        return new Loaded(new Startup(table, durability, mode), new Live(Map.copyOf(recipes), anvil, whitelist));
    }

    private static JsonObject readOrCreate(Path file, JsonObject defaults) {
        if (Files.notExists(file)) {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(defaults) + "\n", StandardCharsets.UTF_8);
                MountArmorTweaks.LOGGER.info("Wrote default config to {}", file);
            } catch (IOException ex) {
                MountArmorTweaks.LOGGER.warn("Could not write default config to {}: {}", file, ex.toString());
            }
            return defaults;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
            MountArmorTweaks.LOGGER.warn("{} is not a JSON object; using defaults", file);
        } catch (Exception ex) {
            MountArmorTweaks.LOGGER.warn("Could not read {}; using defaults: {}", file, ex.toString());
        }
        return defaults;
    }

    // Defaults --------------------------------------------------------------------------------

    private static final List<String> DEFAULT_HORSE = List.of(
        "minecraft:protection", "minecraft:fire_protection", "minecraft:blast_protection",
        "minecraft:projectile_protection", "minecraft:feather_falling"
    );
    private static final List<String> DEFAULT_NAUTILUS = List.of(
        "minecraft:protection", "minecraft:fire_protection", "minecraft:blast_protection",
        "minecraft:projectile_protection"
    );
    private static final List<String> DEFAULT_WITH_DURABILITY = List.of("minecraft:unbreaking", "minecraft:mending");

    private static JsonObject defaultConfig() {
        JsonObject root = new JsonObject();
        JsonObject recipes = new JsonObject();
        RECIPE_ITEMS.forEach(item -> recipes.addProperty(item, true));
        root.add("recipes", recipes);
        JsonObject enchanting = new JsonObject();
        enchanting.addProperty("table", true);
        enchanting.addProperty("anvil", true);
        root.add("enchanting", enchanting);
        root.addProperty("durability", false);
        JsonObject trades = new JsonObject();
        trades.addProperty("mode", "off");
        root.add("trades", trades);
        return root;
    }

    private static JsonObject defaultEnchantments() {
        JsonObject root = new JsonObject();
        root.add("horse_armor", array(DEFAULT_HORSE));
        root.add("nautilus_armor", array(DEFAULT_NAUTILUS));
        root.add("with_durability", array(DEFAULT_WITH_DURABILITY));
        return root;
    }

    private static JsonArray array(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    // Lenient reading -------------------------------------------------------------------------

    /** Reads values with a default and a log line for every value that has the wrong type. */
    private record Reader(String file) {
        JsonObject object(JsonObject parent, String key) {
            JsonElement value = parent.get(key);
            if (value == null) {
                return new JsonObject();
            }
            if (value.isJsonObject()) {
                return value.getAsJsonObject();
            }
            warn(key, "an object");
            return new JsonObject();
        }

        boolean bool(JsonObject parent, String path, String key, boolean fallback) {
            JsonElement value = parent.get(key);
            if (value == null) {
                return fallback;
            }
            if (value instanceof JsonPrimitive p && p.isBoolean()) {
                return p.getAsBoolean();
            }
            warn(path, "true or false");
            return fallback;
        }

        TradesMode tradesMode(JsonObject trades) {
            JsonElement value = trades.get("mode");
            if (value == null) {
                return TradesMode.OFF;
            }
            if (value instanceof JsonPrimitive p && p.isString()) {
                try {
                    return TradesMode.valueOf(p.getAsString().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    // fall through to the warning
                }
            }
            warn("trades.mode", "\"off\", \"horse\", \"nautilus\" or \"both\"");
            return TradesMode.OFF;
        }

        List<String> strings(JsonObject parent, String key, List<String> fallback) {
            JsonElement value = parent.get(key);
            if (value == null) {
                return fallback;
            }
            if (!value.isJsonArray()) {
                warn(key, "a list of enchantment ids");
                return fallback;
            }
            List<String> out = new ArrayList<>();
            for (JsonElement element : value.getAsJsonArray()) {
                if (element instanceof JsonPrimitive p && p.isString()) {
                    out.add(p.getAsString());
                } else {
                    MountArmorTweaks.LOGGER.warn("{}: ignoring non-string entry {} in {}", file, element, key);
                }
            }
            return List.copyOf(out);
        }

        private void warn(String path, String expected) {
            MountArmorTweaks.LOGGER.warn("{}: {} must be {}; using the default", file, path, expected);
        }
    }
}
