package verymc.top.shadow;

import net.minecraft.world.item.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ShadowStorage {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public ShadowStorage(JavaPlugin plugin) {
        this.plugin = plugin;

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.file = new File(plugin.getDataFolder(), "shadows.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save(List<ShadowGroup> groups) {

        config = new YamlConfiguration();

        for (ShadowGroup group : groups) {

            String path = "shadows." + group.getId();

            config.set(
                    path + ".id",
                    group.getId().toString()
            );

            /*
             * Guardamos el ItemStack usando la serialización
             * de Bukkit.
             *
             * Esto conserva la información del item sin depender
             * directamente de las APIs NBT que cambian entre
             * versiones de Minecraft.
             */
            if (group.hasShadowStack()) {

                try {

                    org.bukkit.inventory.ItemStack bukkitStack =
                            CraftItemStack.asBukkitCopy(
                                    group.getShadowStack()
                            );

                    config.set(
                            path + ".item",
                            bukkitStack.serialize()
                    );

                } catch (Exception e) {

                    plugin.getLogger().warning(
                            "No se pudo guardar el ItemStack del Shadow "
                                    + group.getId()
                                    + ": "
                                    + e.getMessage()
                    );
                }
            }

            List<String> endpointData = new ArrayList<>();

            for (ShadowEndpoint endpoint : group.getEndpoints()) {

                if (
                        endpoint.getType()
                                == ShadowEndpoint.Type.PLAYER
                ) {

                    endpointData.add(
                            "PLAYER|"
                                    + endpoint.getPlayerUuid()
                                    + "|"
                                    + endpoint.getPlayerSlot()
                    );

                } else if (
                        endpoint.getType()
                                == ShadowEndpoint.Type.CONTAINER
                ) {

                    endpointData.add(
                            "CONTAINER|"
                                    + endpoint.getWorld()
                                    + "|"
                                    + endpoint.getX()
                                    + "|"
                                    + endpoint.getY()
                                    + "|"
                                    + endpoint.getZ()
                                    + "|"
                                    + endpoint.getContainerSlot()
                    );
                }
            }

            config.set(
                    path + ".endpoints",
                    endpointData
            );
        }

        try {

            config.save(file);

        } catch (IOException e) {

            plugin.getLogger().severe(
                    "No se pudo guardar shadows.yml: "
                            + e.getMessage()
            );
        }
    }

    public List<ShadowGroup> load() {

        List<ShadowGroup> groups = new ArrayList<>();

        ConfigurationSection shadows =
                config.getConfigurationSection("shadows");

        if (shadows == null) {
            return groups;
        }

        for (String key : shadows.getKeys(false)) {

            try {

                UUID id =
                        UUID.fromString(key);

                ShadowGroup group =
                        new ShadowGroup(id);

                /*
                 * Cargar ItemStack serializado por Bukkit.
                 */
                ConfigurationSection itemSection =
                        config.getConfigurationSection(
                                "shadows." + key + ".item"
                        );

                if (itemSection != null) {

                    try {

                        Map<String, Object> itemData =
                                itemSection.getValues(false);

                        org.bukkit.inventory.ItemStack bukkitStack =
                                org.bukkit.inventory.ItemStack.deserialize(
                                        itemData
                                );

                        ItemStack nmsStack =
                                CraftItemStack.asNMSCopy(
                                        bukkitStack
                                );

                        group.setShadowStack(nmsStack);

                    } catch (Exception e) {

                        plugin.getLogger().warning(
                                "No se pudo cargar el ItemStack del Shadow "
                                        + id
                                        + ": "
                                        + e.getMessage()
                        );
                    }
                }

                /*
                 * Cargar endpoints.
                 */
                List<String> endpointData =
                        config.getStringList(
                                "shadows." + key + ".endpoints"
                        );

                for (String data : endpointData) {

                    String[] parts =
                            data.split("\\|");

                    if (parts.length == 0) {
                        continue;
                    }

                    if (
                            "PLAYER".equals(parts[0])
                                    && parts.length == 3
                    ) {

                        UUID playerUuid =
                                UUID.fromString(parts[1]);

                        int slot =
                                Integer.parseInt(parts[2]);

                        group.addEndpoint(
                                ShadowEndpoint.player(
                                        playerUuid,
                                        slot
                                )
                        );

                    } else if (
                            "CONTAINER".equals(parts[0])
                                    && parts.length == 6
                    ) {

                        String world =
                                parts[1];

                        int x =
                                Integer.parseInt(parts[2]);

                        int y =
                                Integer.parseInt(parts[3]);

                        int z =
                                Integer.parseInt(parts[4]);

                        int slot =
                                Integer.parseInt(parts[5]);

                        group.addEndpoint(
                                ShadowEndpoint.container(
                                        world,
                                        x,
                                        y,
                                        z,
                                        slot
                                )
                        );
                    }
                }

                groups.add(group);

            } catch (Exception e) {

                plugin.getLogger().warning(
                        "No se pudo cargar ShadowGroup '"
                                + key
                                + "': "
                                + e.getMessage()
                );
            }
        }

        return groups;
    }

    public File getFile() {
        return file;
    }
}