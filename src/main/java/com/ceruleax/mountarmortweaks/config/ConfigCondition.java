package com.ceruleax.mountarmortweaks.config;

import com.ceruleax.mountarmortweaks.MountArmorTweaks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.Nullable;

/**
 * Load condition {@code mountarmortweaks:config}: keeps a data file only while the named config
 * switch is on. Used in {@code fabric:load_conditions} of the mod's recipes, recipe advancements and
 * villager trades, so a disabled entry never enters its registry.
 *
 * <p>{@code feature} values:
 * <ul>
 *   <li>{@code recipe/<item>}: {@code recipes.<item>} (follows {@code /reload})</li>
 *   <li>{@code trades/horse}, {@code trades/nautilus}: {@code trades.mode} (restart)</li>
 *   <li>{@code enchanting_table}: {@code enchanting.table} (restart)</li>
 * </ul>
 */
public record ConfigCondition(String feature) implements ResourceCondition {
    public static final MapCodec<ConfigCondition> CODEC =
        Codec.STRING.fieldOf("feature").xmap(ConfigCondition::new, ConfigCondition::feature);

    public static final ResourceConditionType<ConfigCondition> TYPE =
        ResourceConditionType.create(Identifier.fromNamespaceAndPath(MountArmorTweaks.MOD_ID, "config"), CODEC);

    private static final String RECIPE_PREFIX = "recipe/";

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(RegistryOps.@Nullable RegistryInfoLookup registryInfo) {
        if (feature.startsWith(RECIPE_PREFIX)) {
            String item = feature.substring(RECIPE_PREFIX.length());
            if (Config.RECIPE_ITEMS.contains(item)) {
                return Config.live().recipe(item);
            }
        } else {
            switch (feature) {
                case "trades/horse" -> {
                    return Config.startup().trades().horse();
                }
                case "trades/nautilus" -> {
                    return Config.startup().trades().nautilus();
                }
                case "enchanting_table" -> {
                    return Config.startup().enchantingTable();
                }
                default -> {
                }
            }
        }
        MountArmorTweaks.LOGGER.warn("Unknown {} feature \"{}\"; the entry is not loaded", TYPE.id(), feature);
        return false;
    }
}
