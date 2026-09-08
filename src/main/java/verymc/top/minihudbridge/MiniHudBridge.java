package verymc.top.minihudbridge;

import org.bukkit.plugin.java.JavaPlugin;

public final class MiniHudBridge extends JavaPlugin {

    private ServuxStructuresHandler structuresHandler;

    @Override
    public void onEnable() {
        getLogger().info("Iniciando MiniHudBridge...");

        structuresHandler = new ServuxStructuresHandler(this);
        structuresHandler.register();

        getLogger().info("Canal Servux structures registrado.");
        getLogger().info("MiniHudBridge iniciado correctamente.");
    }

    @Override
    public void onDisable() {
        if (structuresHandler != null) {
            structuresHandler.unregister();
        }

        getLogger().info("MiniHudBridge detenido.");
    }

    public ServuxStructuresHandler getStructuresHandler() {
        return structuresHandler;
    }
}