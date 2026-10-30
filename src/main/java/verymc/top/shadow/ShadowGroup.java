package verymc.top.shadow;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ShadowGroup {

private final UUID id;
private final List<ShadowEndpoint> endpoints;

/*
 * Este ItemStack será la instancia compartida en memoria
 * cuando el Shadow esté activo.
 */
private ItemStack shadowStack;

public ShadowGroup(UUID id) {
    this.id = id;
    this.endpoints = new ArrayList<>();
    this.shadowStack = ItemStack.EMPTY;
}

public UUID getId() {
    return id;
}

public void addEndpoint(ShadowEndpoint endpoint) {
    if (!endpoints.contains(endpoint)) {
        endpoints.add(endpoint);
    }
}

public void removeEndpoint(ShadowEndpoint endpoint) {
    endpoints.remove(endpoint);
}

public List<ShadowEndpoint> getEndpoints() {
    return Collections.unmodifiableList(endpoints);
}

public ItemStack getShadowStack() {
    return shadowStack;
}

public void setShadowStack(ItemStack shadowStack) {
    this.shadowStack = shadowStack;
}

public boolean hasShadowStack() {
    return shadowStack != null && !shadowStack.isEmpty();
}


}
