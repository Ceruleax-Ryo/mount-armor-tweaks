package com.ceruleax.mountarmortweaks;

import com.ceruleax.mountarmortweaks.config.Config;
import com.ceruleax.mountarmortweaks.network.ComponentsPayload;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Default components added to mount armor, through Fabric API's {@code DefaultItemComponentEvents.MODIFY}.
 *
 * <p>That event runs every time the server binds item components: at start-up and on every
 * {@code /reload}. The values come from {@link Config#startup()}, which is fixed until the server stops,
 * so all stacks in a running world share the same defaults.
 *
 * <p>It also runs on a client that joins a remote server, which rebuilds item components itself. There
 * the values come from the server ({@link RemoteServer}); a server without the mod means vanilla
 * components. Clients without the mod see vanilla components (no durability bar).
 *
 * <ul>
 *   <li>{@code enchanting.table}: {@code enchantable} with the vanilla armor material's enchantability.</li>
 *   <li>{@code durability}: {@code max_damage} (wolf armor formula: material durability × 16),
 *       {@code damage}, {@code repairable} (the material's repair tag), and {@code equippable} with
 *       {@code damage_on_hurt} switched on.</li>
 * </ul>
 *
 * <p>When durability is off, stacks keep any {@code damage} they had but are not damageable, so
 * Unbreaking and Mending on them do nothing.
 */
public final class Components {
    private Components() {
    }

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            // A client on a remote server follows the server; everywhere else the local config applies.
            ComponentsPayload remote = RemoteServer.components();
            boolean table;
            boolean durability;
            if (remote != null) {
                table = remote.enchantingTable();
                durability = remote.durability();
            } else {
                Config.Startup startup = Config.startup();
                table = startup.enchantingTable();
                durability = startup.durability();
            }
            if (!table && !durability) {
                return;
            }
            for (MountArmor.Entry entry : MountArmor.all()) {
                context.modify(entry.item(), (builder, registries, item) -> apply(entry, table, durability, builder, registries));
            }
        });
    }

    private static void apply(MountArmor.Entry entry, boolean table, boolean durability, DataComponentMap.Builder builder, HolderLookup.Provider registries) {
        if (table) {
            builder.set(DataComponents.ENCHANTABLE, new Enchantable(entry.material().enchantmentValue()));
        }
        if (durability) {
            builder.set(DataComponents.MAX_DAMAGE, ArmorType.BODY.getDurability(entry.material().durability()));
            builder.set(DataComponents.DAMAGE, 0);
            builder.set(DataComponents.MAX_STACK_SIZE, 1);
            builder.set(DataComponents.REPAIRABLE, new Repairable(
                registries.lookupOrThrow(Registries.ITEM).getOrThrow(entry.material().repairIngredient())
            ));
            Equippable equippable = builder.get(DataComponents.EQUIPPABLE);
            if (equippable != null && !equippable.damageOnHurt()) {
                builder.set(DataComponents.EQUIPPABLE, new Equippable(
                    equippable.slot(),
                    equippable.equipSound(),
                    equippable.assetId(),
                    equippable.cameraOverlay(),
                    equippable.allowedEntities(),
                    equippable.dispensable(),
                    equippable.swappable(),
                    true,
                    equippable.equipOnInteract(),
                    equippable.canBeSheared(),
                    equippable.shearingSound()
                ));
            }
        }
    }
}
