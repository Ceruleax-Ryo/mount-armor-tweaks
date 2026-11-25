package com.ceruleax.mountarmortweaks;

import com.ceruleax.mountarmortweaks.config.Config;
import java.util.List;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;

/**
 * Which enchantments mount armor accepts.
 *
 * <p>Two paths reach {@link #allow}, through Fabric API's {@code ALLOW_ENCHANTING} event:
 * <ul>
 *   <li>{@link EnchantingContext#PRIMARY}: the enchanting table and {@code enchant_with_levels}
 *       (used by villager trades). Both also need the {@code enchantable} component, which is only
 *       present while {@code enchanting.table} is on.</li>
 *   <li>{@link EnchantingContext#ACCEPTABLE}: the anvil, {@code /enchant} and {@code enchant_randomly}.
 *       Governed by {@code enchanting.anvil}.</li>
 * </ul>
 *
 * <p>Hard rules, which the whitelist cannot override:
 * <ol>
 *   <li>The enchantment must apply to the body slot ({@code slots} contains {@code armor} or
 *       {@code any}). This also covers enchantments added by other mods.</li>
 *   <li>An enchantment that changes item damage or repairs from XP (Unbreaking, Mending and modded
 *       equivalents) is only allowed while {@code durability} is on.</li>
 * </ol>
 *
 * <p>Otherwise the answer is {@link TriState#DEFAULT}, never {@code FALSE}: other mods keep the final say.
 */
public final class Enchanting {
    private Enchanting() {
    }

    public static TriState allow(Holder<Enchantment> enchantment, ItemStack stack, EnchantingContext context) {
        MountArmor.Entry entry = MountArmor.get(stack.getItem());
        if (entry == null) {
            return TriState.DEFAULT;
        }
        Config.Live live = Config.live();
        if (context == EnchantingContext.ACCEPTABLE && !live.anvil()) {
            return TriState.DEFAULT;
        }
        if (!fitsBody(enchantment.value())) {
            return TriState.DEFAULT;
        }
        boolean durabilityDependent = dependsOnDurability(enchantment.value());
        if (durabilityDependent && !Config.startup().durability()) {
            return TriState.DEFAULT;
        }
        Config.Whitelist whitelist = live.whitelist();
        List<String> list = entry.kind() == MountArmor.Kind.HORSE ? whitelist.horseArmor() : whitelist.nautilusArmor();
        if (matches(enchantment, list) || durabilityDependent && matches(enchantment, whitelist.withDurability())) {
            return TriState.TRUE;
        }
        return TriState.DEFAULT;
    }

    static boolean fitsBody(Enchantment enchantment) {
        return enchantment.matchingSlot(EquipmentSlot.BODY);
    }

    static boolean dependsOnDurability(Enchantment enchantment) {
        return !enchantment.getEffects(EnchantmentEffectComponents.ITEM_DAMAGE).isEmpty()
            || !enchantment.getEffects(EnchantmentEffectComponents.REPAIR_WITH_XP).isEmpty();
    }

    private static boolean matches(Holder<Enchantment> enchantment, List<String> entries) {
        for (String raw : entries) {
            boolean tag = raw.startsWith("#");
            Identifier id = Identifier.tryParse(tag ? raw.substring(1) : raw);
            if (id == null) {
                continue;
            }
            if (tag ? enchantment.is(TagKey.create(Registries.ENCHANTMENT, id)) : enchantment.is(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Logs whitelist entries that can never take effect: unknown ids or tags, enchantments that do
     * not apply to the body slot, and durability-dependent enchantments outside {@code with_durability}.
     */
    public static void validate(HolderLookup.Provider registries) {
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Config.Whitelist whitelist = Config.live().whitelist();
        check(lookup, "horse_armor", whitelist.horseArmor(), false);
        check(lookup, "nautilus_armor", whitelist.nautilusArmor(), false);
        check(lookup, "with_durability", whitelist.withDurability(), true);
    }

    private static void check(HolderLookup.RegistryLookup<Enchantment> lookup, String key, List<String> entries, boolean durabilityList) {
        for (String raw : entries) {
            boolean tag = raw.startsWith("#");
            Identifier id = Identifier.tryParse(tag ? raw.substring(1) : raw);
            if (id == null) {
                warn(key, raw, "is not a valid id");
                continue;
            }
            List<Holder<Enchantment>> holders;
            if (tag) {
                var set = lookup.get(TagKey.create(Registries.ENCHANTMENT, id));
                if (set.isEmpty()) {
                    warn(key, raw, "is not a known enchantment tag");
                    continue;
                }
                holders = set.get().stream().toList();
            } else {
                var holder = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, id));
                if (holder.isEmpty()) {
                    warn(key, raw, "is not a known enchantment");
                    continue;
                }
                holders = List.of(holder.get());
            }
            for (Holder<Enchantment> holder : holders) {
                String name = holder.unwrapKey().map(k -> k.identifier().toString()).orElse(raw);
                if (!fitsBody(holder.value())) {
                    warn(key, name, "does not apply to the body slot and is ignored");
                } else if (!durabilityList && dependsOnDurability(holder.value())) {
                    warn(key, name, "depends on durability; list it under with_durability instead");
                }
            }
        }
    }

    private static void warn(String key, String entry, String problem) {
        MountArmorTweaks.LOGGER.warn("{}: {} entry {} {}", Config.ENCHANTMENTS_FILE, key, entry, problem);
    }
}
