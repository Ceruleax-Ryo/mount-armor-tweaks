package com.ceruleax.mountarmortweaks;

import com.ceruleax.mountarmortweaks.network.ComponentsPayload;
import org.jspecify.annotations.Nullable;

/**
 * Component switches received from a remote server. {@code null} whenever the item components on
 * this side are built from the local config: on a dedicated server, and in singleplayer (the
 * integrated server binds components for the whole game, and the client does not rebuild them).
 *
 * <p>Set and cleared by {@code MountArmorTweaksClient}; kept here so that common code never loads
 * client classes.
 */
public final class RemoteServer {
    private static volatile @Nullable ComponentsPayload components;

    private RemoteServer() {
    }

    public static @Nullable ComponentsPayload components() {
        return components;
    }

    public static void set(@Nullable ComponentsPayload value) {
        components = value;
    }
}
