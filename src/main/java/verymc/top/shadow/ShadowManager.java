package verymc.top.shadow;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ShadowManager {

    private final JavaPlugin plugin;
    private final ShadowStorage storage;

    private final List<ShadowGroup> groups =
            new ArrayList<>();

    public ShadowManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storage = new ShadowStorage(plugin);

        load();
    }

    /**
     * Carga todos los Shadows guardados en shadows.yml.
     */
    public void load() {

        groups.clear();

        groups.addAll(
                storage.load()
        );

        plugin.getLogger().info(
                "Shadow Items cargados: "
                        + groups.size()
        );
    }

    /**
     * Guarda todos los Shadows actuales.
     */
    public void save() {
        storage.save(groups);
    }

    /**
     * Crea un nuevo ShadowGroup.
     */
    public ShadowGroup createGroup() {

        ShadowGroup group =
                new ShadowGroup(
                        UUID.randomUUID()
                );

        groups.add(group);

        save();

        return group;
    }

    /**
     * Elimina un ShadowGroup.
     */
    public boolean removeGroup(UUID id) {

        boolean removed =
                groups.removeIf(
                        group ->
                                group.getId()
                                        .equals(id)
                );

        if (removed) {
            save();
        }

        return removed;
    }

    /**
     * Elimina TODOS los ShadowGroups.
     *
     * No modifica físicamente los ítems.
     * Solamente elimina las sincronizaciones.
     */
    public int removeAllGroups() {

        int count =
                groups.size();

        groups.clear();

        save();

        return count;
    }

    /**
     * Busca un Shadow por su UUID.
     */
    public ShadowGroup getGroup(UUID id) {

        for (ShadowGroup group : groups) {

            if (
                    group.getId()
                            .equals(id)
            ) {
                return group;
            }
        }

        return null;
    }

    /**
     * Devuelve todos los Shadows.
     */
    public List<ShadowGroup> getGroups() {
        return List.copyOf(groups);
    }

    /**
     * Busca un Shadow que pertenezca a un slot
     * concreto del inventario de un jugador.
     */
    public ShadowGroup findPlayerShadow(
            UUID playerUuid,
            int slot
    ) {

        for (ShadowGroup group : groups) {

            for (
                    ShadowEndpoint endpoint :
                            group.getEndpoints()
            ) {

                if (
                        endpoint.getType()
                                == ShadowEndpoint.Type.PLAYER
                                &&
                        playerUuid.equals(
                                endpoint.getPlayerUuid()
                        )
                                &&
                        endpoint.getPlayerSlot()
                                        == slot
                ) {
                    return group;
                }
            }
        }

        return null;
    }

    /**
     * Busca un Shadow asociado a un bloque/contenedor.
     */
    public ShadowGroup findContainerShadow(
            String world,
            int x,
            int y,
            int z,
            int slot
    ) {

        for (ShadowGroup group : groups) {

            for (
                    ShadowEndpoint endpoint :
                            group.getEndpoints()
            ) {

                if (
                        endpoint.getType()
                                == ShadowEndpoint.Type.CONTAINER
                                &&
                        world.equals(
                                endpoint.getWorld()
                        )
                                &&
                        endpoint.getX() == x
                                &&
                        endpoint.getY() == y
                                &&
                        endpoint.getZ() == z
                                &&
                        endpoint.getContainerSlot()
                                        == slot
                ) {
                    return group;
                }
            }
        }

        return null;
    }

    public ShadowStorage getStorage() {
        return storage;
    }
}