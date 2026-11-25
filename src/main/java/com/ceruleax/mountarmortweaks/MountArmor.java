package com.ceruleax.mountarmortweaks;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import org.jspecify.annotations.Nullable;

/** The vanilla mount armor items this mod works on, with their armor type and material. */
public final class MountArmor {
    public enum Kind {
        HORSE("horse_armor"),
        NAUTILUS("nautilus_armor");

        /** Key used for this kind in {@code enchantments.json}. */
        public final String configKey;

        Kind(String configKey) {
            this.configKey = configKey;
        }
    }

    public record Entry(Item item, Kind kind, ArmorMaterial material) {
        /** Registry path, e.g. {@code iron_horse_armor}. */
        public String name() {
            return BuiltInRegistries.ITEM.getKey(item).getPath();
        }
    }

    private static final Map<Item, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        add(Items.LEATHER_HORSE_ARMOR, Kind.HORSE, ArmorMaterials.LEATHER);
        add(Items.COPPER_HORSE_ARMOR, Kind.HORSE, ArmorMaterials.COPPER);
        add(Items.IRON_HORSE_ARMOR, Kind.HORSE, ArmorMaterials.IRON);
        add(Items.GOLDEN_HORSE_ARMOR, Kind.HORSE, ArmorMaterials.GOLD);
        add(Items.DIAMOND_HORSE_ARMOR, Kind.HORSE, ArmorMaterials.DIAMOND);
        add(Items.NETHERITE_HORSE_ARMOR, Kind.HORSE, ArmorMaterials.NETHERITE);
        add(Items.COPPER_NAUTILUS_ARMOR, Kind.NAUTILUS, ArmorMaterials.COPPER);
        add(Items.IRON_NAUTILUS_ARMOR, Kind.NAUTILUS, ArmorMaterials.IRON);
        add(Items.GOLDEN_NAUTILUS_ARMOR, Kind.NAUTILUS, ArmorMaterials.GOLD);
        add(Items.DIAMOND_NAUTILUS_ARMOR, Kind.NAUTILUS, ArmorMaterials.DIAMOND);
        add(Items.NETHERITE_NAUTILUS_ARMOR, Kind.NAUTILUS, ArmorMaterials.NETHERITE);
    }

    private static void add(Item item, Kind kind, ArmorMaterial material) {
        ENTRIES.put(item, new Entry(item, kind, material));
    }

    private MountArmor() {
    }

    public static @Nullable Entry get(Item item) {
        return ENTRIES.get(item);
    }

    public static Iterable<Entry> all() {
        return ENTRIES.values();
    }
}
