package com.ceruleax.mountarmortweaks.network;

import com.ceruleax.mountarmortweaks.MountArmorTweaks;
import com.ceruleax.mountarmortweaks.config.Config;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Tells a client that also runs this mod which item components the server adds.
 *
 * <p>A client connected to a remote server rebuilds item components itself. Without this packet a
 * modded client would apply its own local config, which may not match the server. The packet is sent
 * during the configuration phase, before the client rebuilds components. Clients without the mod do
 * not announce the channel and never receive it.
 */
public record ComponentsPayload(boolean enchantingTable, boolean durability) implements CustomPacketPayload {
    public static final Type<ComponentsPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MountArmorTweaks.MOD_ID, "components"));

    public static final StreamCodec<FriendlyByteBuf, ComponentsPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, ComponentsPayload::enchantingTable,
        ByteBufCodecs.BOOL, ComponentsPayload::durability,
        ComponentsPayload::new
    );

    /** What a modded client assumes until the server says otherwise: vanilla components. */
    public static final ComponentsPayload VANILLA = new ComponentsPayload(false, false);

    @Override
    public Type<ComponentsPayload> type() {
        return TYPE;
    }

    public static void registerCommon() {
        PayloadTypeRegistry.clientboundConfiguration().register(TYPE, CODEC);
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (ServerConfigurationNetworking.canSend(handler, TYPE)) {
                Config.Startup s = Config.startup();
                ServerConfigurationNetworking.send(handler, new ComponentsPayload(s.enchantingTable(), s.durability()));
            }
        });
    }
}
