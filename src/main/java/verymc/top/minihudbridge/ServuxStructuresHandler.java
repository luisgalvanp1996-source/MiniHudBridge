package verymc.top.minihudbridge;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServuxStructuresHandler
        implements Listener, PluginMessageListener {

    public static final String CHANNEL = "servux:structures";

    private final MiniHudBridge plugin;
    private final Set<UUID> registeredPlayers = new HashSet<>();

    private StructureDataProvider provider;

    public ServuxStructuresHandler(MiniHudBridge plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getMessenger()
                .registerIncomingPluginChannel(
                        plugin,
                        CHANNEL,
                        this
                );

        plugin.getServer().getMessenger()
                .registerOutgoingPluginChannel(
                        plugin,
                        CHANNEL
                );

        plugin.getServer().getPluginManager()
                .registerEvents(this, plugin);

        provider = new StructureDataProvider(plugin, this);

        provider.start();
    }

    public void unregister() {
        if (provider != null) {
            provider.stop();
        }

        plugin.getServer().getMessenger()
                .unregisterIncomingPluginChannel(
                        plugin,
                        CHANNEL
                );

        plugin.getServer().getMessenger()
                .unregisterOutgoingPluginChannel(
                        plugin,
                        CHANNEL
                );

        registeredPlayers.clear();
    }

    @Override
    public void onPluginMessageReceived(
            String channel,
            Player player,
            byte[] message
    ) {
        if (!CHANNEL.equals(channel)) {
            return;
        }

        try {
            ServuxStructuresPacket packet =
                    ServuxStructuresPacket.decode(message);

            switch (packet.getType()) {

                case PACKET_C2S_STRUCTURES_REGISTER:
                    registerPlayer(player);
                    break;

                case PACKET_C2S_STRUCTURES_UNREGISTER:
                    unregisterPlayer(player);
                    break;

                default:
                    break;
            }

        } catch (Exception e) {
            plugin.getLogger().warning(
                    "Error procesando Servux structures de "
                            + player.getName()
                            + ": "
                            + e.getMessage()
            );
        }
    }

    private void registerPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        if (!registeredPlayers.add(uuid)) {
            return;
        }

        plugin.getLogger().info(
                player.getName()
                        + " registró Servux structures."
        );

        sendMetadata(player);

        provider.initialSync(player);
    }

    private void unregisterPlayer(Player player) {
        registeredPlayers.remove(player.getUniqueId());

        if (provider != null) {
            provider.unregister(player);
        }
    }

    public boolean isRegistered(Player player) {
        return registeredPlayers.contains(player.getUniqueId());
    }

    public void sendMetadata(Player player) {
        ServuxStructuresPacket packet =
                new ServuxStructuresPacket(
                        ServuxStructuresPacket.Type.PACKET_S2C_METADATA,
                        createMetadata()
                );

        sendPacket(player, packet);
    }


private net.minecraft.nbt.CompoundTag createMetadata() {
    net.minecraft.nbt.CompoundTag nbt =
            new net.minecraft.nbt.CompoundTag();

    nbt.putString(
            "name",
            "structure_bounding_boxes"
    );

    nbt.putString(
            "id",
            CHANNEL
    );

    nbt.putInt(
            "version",
            ServuxStructuresPacket.PROTOCOL_VERSION
    );

    // MiniHUD Fabric para Minecraft 26.2
    nbt.putString(
            "servux",
            "servux-fabric-26.2-0.11.1"
    );

    nbt.putInt(
            "timeout",
            600
    );

    return nbt;
}


    public void sendPacket(
        Player player,
        ServuxStructuresPacket packet
) {
    byte[] data = packet.encode();

    plugin.getLogger().info(
            "S2C -> "
                    + player.getName()
                    + " | Tipo: "
                    + packet.getType()
                    + " ("
                    + packet.getType().getId()
                    + ")"
                    + " | Bytes: "
                    + data.length
    );

    player.sendPluginMessage(
            plugin,
            CHANNEL,
            data
    );
}
    public void sendStructureData(
            Player player,
            byte[] data
    ) {
        PacketSplitter.send(
                data,
                fragment -> {

                    ServuxStructuresPacket packet =
                            new ServuxStructuresPacket(
                                    ServuxStructuresPacket.Type.PACKET_S2C_STRUCTURE_DATA,
                                    fragment
                            );

                    sendPacket(player, packet);
                }
        );
    }

public void sendStructureStart(
        Player player,
        net.minecraft.nbt.CompoundTag nbt
) {
    try {

        byte[] data =
                ServuxDataSerializer.serialize(nbt);

        plugin.getLogger().info(
                "Servux STRUCTURE_DATA preparado: "
                        + data.length
                        + " bytes"
        );

        PacketSplitter.send(
                data,
                fragment -> {

                    ServuxStructuresPacket packet =
                            new ServuxStructuresPacket(
                                    ServuxStructuresPacket.Type.PACKET_S2C_STRUCTURE_DATA,
                                    fragment
                            );

                    sendPacket(
                            player,
                            packet
                    );
                }
        );

    } catch (Exception e) {

        plugin.getLogger().severe(
                "Error serializando estructuras Servux: "
                        + e.getMessage()
        );

        e.printStackTrace();
    }
}}