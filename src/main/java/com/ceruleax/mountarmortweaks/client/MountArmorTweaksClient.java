package com.ceruleax.mountarmortweaks.client;

import com.ceruleax.mountarmortweaks.RemoteServer;
import com.ceruleax.mountarmortweaks.network.ComponentsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * Optional client part: when this client joins a remote server, use the server's component
 * switches instead of the local config, so that durability bars and enchanting previews match.
 * Servers without the mod send nothing, and the client keeps vanilla components.
 */
public final class MountArmorTweaksClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientConfigurationConnectionEvents.INIT.register((handler, client) ->
            RemoteServer.set(client.hasSingleplayerServer() ? null : ComponentsPayload.VANILLA));
        ClientConfigurationNetworking.registerGlobalReceiver(ComponentsPayload.TYPE, (payload, context) -> {
            if (RemoteServer.components() != null) {
                RemoteServer.set(payload);
            }
        });
        ClientConfigurationConnectionEvents.DISCONNECT.register((handler, client) -> RemoteServer.set(null));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RemoteServer.set(null));
    }
}
