package verymc.top.minihudbridge;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StructureDataProvider {

    private final MiniHudBridge plugin;
    private final ServuxStructuresHandler handler;

    private BukkitTask task;

    /*
     * Cada cuántos ticks hacemos un escaneo.
     *
     * 40 ticks = aproximadamente 2 segundos.
     */
    private static final int UPDATE_INTERVAL = 40;

    /*
     * View distance + margen.
     */
    private static final int EXTRA_CHUNKS = 2;

    /*
     * Evita enviar exactamente la misma estructura
     * continuamente al cliente.
     */
    private final Map<UUID, Set<String>> lastStructures = new HashMap<>();

    public StructureDataProvider(
            MiniHudBridge plugin,
            ServuxStructuresHandler handler
    ) {
        this.plugin = plugin;
        this.handler = handler;
    }

    public void start() {

        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::tick,
                40L,
                UPDATE_INTERVAL
        );

        plugin.getLogger().info(
                "StructureDataProvider iniciado."
        );
    }

    public void stop() {

        if (task != null) {
            task.cancel();
            task = null;
        }

        lastStructures.clear();
    }

    private void tick() {

        for (Player bukkitPlayer :
                Bukkit.getOnlinePlayers()) {

            if (!handler.isRegistered(bukkitPlayer)) {
                continue;
            }

            ServerPlayer player =
                    getServerPlayer(bukkitPlayer);

            if (player == null) {
                continue;
            }

            scanPlayer(player);
        }
    }

    public void initialSync(Player bukkitPlayer) {

        ServerPlayer player =
                getServerPlayer(bukkitPlayer);

        if (player == null) {
            return;
        }

        /*
         * Esperamos un tick para asegurarnos de que
         * el cliente ya recibió el metadata.
         */
        Bukkit.getScheduler().runTask(
                plugin,
                () -> scanPlayer(player)
        );
    }

    public void unregister(Player player) {

        lastStructures.remove(
                player.getUniqueId()
        );
    }

    private ServerPlayer getServerPlayer(Player player) {

        if (!(player instanceof org.bukkit.craftbukkit.entity.CraftPlayer craftPlayer)) {
            return null;
        }

        return craftPlayer.getHandle();
    }

    private void scanPlayer(ServerPlayer player) {

        if (!handler.isRegistered(
                player.getBukkitEntity()
        )) {
            return;
        }

        ServerLevel world =
                (ServerLevel) player.level();

        ChunkPos center =
                player.getLastSectionPos().chunk();

        int radius =
                Bukkit.getViewDistance() + EXTRA_CHUNKS;

        Map<Structure, LongSet> references =
                getStructureReferencesWithinRange(
                        world,
                        center,
                        radius
                );

        Map<ChunkPos, StructureStart> starts =
                getStructureStartsFromReferences(
                        world,
                        references
                );

        if (starts.isEmpty()) {
            return;
        }

        ListTag structures =
                getStructureList(
                        starts,
                        world
                );

        if (structures.isEmpty()) {
            return;
        }

        /*
         * Construimos una firma sencilla para evitar
         * mandar exactamente el mismo conjunto de
         * estructuras cada 2 segundos.
         */
        Set<String> current =
                createStructureKeys(starts);

        UUID uuid = player.getUUID();

        Set<String> previous =
                lastStructures.get(uuid);

        if (current.equals(previous)) {
            return;
        }

        lastStructures.put(
                uuid,
                current
        );

        CompoundTag nbt =
                new CompoundTag();

        nbt.put(
                "Structures",
                structures
        );

plugin.getLogger().info(
        "Enviando estructuras a " + player.getName()
                + ": " + structures.size()
);

        handler.sendStructureStart(
                player.getBukkitEntity(),
                nbt
        );

        plugin.getLogger().fine(
                "Enviadas "
                        + structures.size()
                        + " estructuras a "
                        + player.getName().getString()
        );
    }

    private Set<String> createStructureKeys(
            Map<ChunkPos, StructureStart> starts
    ) {

        Set<String> result =
                new HashSet<>();

        for (Map.Entry<ChunkPos, StructureStart> entry :
                starts.entrySet()) {

            ChunkPos pos =
                    entry.getKey();

            StructureStart start =
                    entry.getValue();

            Structure structure =
                    start.getStructure();

            if (structure == null) {
                continue;
            }

            Identifier id =
                    BuiltInRegistries.STRUCTURE_TYPE
                            .getKey(structure.type());

            if (id == null) {
                continue;
            }

            result.add(
                    id
                            + "@"
                            + pos.x()
                            + ","
                            + pos.z()
            );
        }

        return result;
    }

    private void getStructureReferencesFromChunk(
            int chunkX,
            int chunkZ,
            Level world,
            Map<Structure, LongSet> references
    ) {

        if (!world.hasChunk(chunkX, chunkZ)) {
            return;
        }

        ChunkAccess chunk =
                world.getChunk(
                        chunkX,
                        chunkZ,
                        ChunkStatus.STRUCTURE_REFERENCES,
                        false
                );

        if (chunk == null) {
            return;
        }

        for (Map.Entry<Structure, LongSet> entry :
                chunk.getAllReferences().entrySet()) {

            Structure structure =
                    entry.getKey();

            LongSet startChunks =
                    entry.getValue();

            if (startChunks.isEmpty()) {
                continue;
            }

            references.merge(
                    structure,
                    startChunks,
                    (oldSet, newSet) -> {

                        LongOpenHashSet merged =
                                new LongOpenHashSet(
                                        oldSet
                                );

                        merged.addAll(
                                newSet
                        );

                        return merged;
                    }
            );
        }
    }

    private Map<Structure, LongSet>
    getStructureReferencesWithinRange(
            ServerLevel world,
            ChunkPos center,
            int radius
    ) {

        Map<Structure, LongSet> references =
                new HashMap<>();

        for (
                int x = center.x() - radius;
                x <= center.x() + radius;
                x++
        ) {

            for (
                    int z = center.z() - radius;
                    z <= center.z() + radius;
                    z++
            ) {

                getStructureReferencesFromChunk(
                        x,
                        z,
                        world,
                        references
                );
            }
        }

        return references;
    }

    private Map<ChunkPos, StructureStart>
    getStructureStartsFromReferences(
            ServerLevel world,
            Map<Structure, LongSet> references
    ) {

        Map<ChunkPos, StructureStart> starts =
                new HashMap<>();

        for (Map.Entry<Structure, LongSet> entry :
                references.entrySet()) {

            Structure structure =
                    entry.getKey();

            LongSet startChunks =
                    entry.getValue();

            LongIterator iterator =
                    startChunks.iterator();

            while (iterator.hasNext()) {

                ChunkPos pos =
                        ChunkPos.unpack(
                                iterator.nextLong()
                        );

                if (!world.hasChunk(
                        pos.x(),
                        pos.z()
                )) {
                    continue;
                }

                ChunkAccess chunk =
                        world.getChunk(
                                pos.x(),
                                pos.z(),
                                ChunkStatus.STRUCTURE_REFERENCES,
                                false
                        );

                if (chunk == null) {
                    continue;
                }

                StructureStart start =
                        chunk.getStartForStructure(
                                structure
                        );

                if (start != null) {

                    starts.put(
                            pos,
                            start
                    );
                }
            }
        }

        return starts;
    }

    private ListTag getStructureList(
            Map<ChunkPos, StructureStart> structures,
            ServerLevel world
    ) {

        ListTag list =
                new ListTag();

        StructurePieceSerializationContext context =
                StructurePieceSerializationContext.fromLevel(
                        world
                );

        for (Map.Entry<ChunkPos, StructureStart> entry :
                structures.entrySet()) {

            StructureStart start =
                    entry.getValue();

            Structure structure =
                    start.getStructure();

            if (structure == null) {
                continue;
            }

            Identifier structureType =
                    BuiltInRegistries.STRUCTURE_TYPE
                            .getKey(structure.type());

            if (structureType == null) {
                continue;
            }

            boolean expandBox =
                    structure.terrainAdaptation()
                            != TerrainAdjustment.NONE;

            CompoundTag nbt =
                    start.createTag(
                            context,
                            entry.getKey()
                    );

            nbt.putBoolean(
                    "ExpandBox",
                    expandBox
            );

            list.add(nbt);
        }

        return list;
    }
}