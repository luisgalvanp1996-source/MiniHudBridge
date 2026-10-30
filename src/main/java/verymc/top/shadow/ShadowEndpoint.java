package verymc.top.shadow;

import java.util.UUID;

public final class ShadowEndpoint {


public enum Type {
    PLAYER,
    CONTAINER
}

private final Type type;

// PLAYER
private final UUID playerUuid;
private final int playerSlot;

// CONTAINER
private final String world;
private final int x;
private final int y;
private final int z;
private final int containerSlot;

private ShadowEndpoint(
        Type type,
        UUID playerUuid,
        int playerSlot,
        String world,
        int x,
        int y,
        int z,
        int containerSlot
) {
    this.type = type;
    this.playerUuid = playerUuid;
    this.playerSlot = playerSlot;
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
    this.containerSlot = containerSlot;
}

public static ShadowEndpoint player(UUID uuid, int slot) {
    return new ShadowEndpoint(
            Type.PLAYER,
            uuid,
            slot,
            null,
            0,
            0,
            0,
            -1
    );
}

public static ShadowEndpoint container(
        String world,
        int x,
        int y,
        int z,
        int slot
) {
    return new ShadowEndpoint(
            Type.CONTAINER,
            null,
            -1,
            world,
            x,
            y,
            z,
            slot
    );
}

public Type getType() {
    return type;
}

public UUID getPlayerUuid() {
    return playerUuid;
}

public int getPlayerSlot() {
    return playerSlot;
}

public String getWorld() {
    return world;
}

public int getX() {
    return x;
}

public int getY() {
    return y;
}

public int getZ() {
    return z;
}

public int getContainerSlot() {
    return containerSlot;
}


}
