package com.ceruleax.mountarmortweaks;

import com.ceruleax.mountarmortweaks.config.Config;
import com.ceruleax.mountarmortweaks.config.ConfigCondition;
import com.ceruleax.mountarmortweaks.network.ComponentsPayload;
import java.util.Locale;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MountArmorTweaks implements ModInitializer {
    public static final String MOD_ID = "mountarmortweaks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ResourceConditions.register(ConfigCondition.TYPE);
        Components.register();
        ComponentsPayload.registerCommon();
        EnchantmentEvents.ALLOW_ENCHANTING.register(Enchanting::allow);

        // Re-read the files before /reload loads recipes; keep start-up settings until the server stops.
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resources) -> Config.reloadLive());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) {
                Enchanting.validate(server.registryAccess());
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Config.Startup s = Config.startup();
            LOGGER.info("enchanting.table={}, durability={}, trades.mode={}",
                s.enchantingTable(), s.durability(), s.trades().name().toLowerCase(Locale.ROOT));
            Enchanting.validate(server.registryAccess());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Config.reset());
    }
}
