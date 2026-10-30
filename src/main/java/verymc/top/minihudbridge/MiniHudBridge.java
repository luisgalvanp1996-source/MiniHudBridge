package verymc.top.minihudbridge;

import org.bukkit.plugin.java.JavaPlugin;
import verymc.top.shadow.ShadowCommand;
import verymc.top.shadow.ShadowManager;

public final class MiniHudBridge extends JavaPlugin {

    private ServuxStructuresHandler structuresHandler;
    private ShadowManager shadowManager;

    @Override
    public void onEnable() {
        getLogger().info("Iniciando MiniHudBridge...");

        structuresHandler = new ServuxStructuresHandler(this);
        structuresHandler.register();

        getLogger().info("Canal Servux structures registrado.");

        // Shadow Items
        shadowManager = new ShadowManager(this);

        if (getCommand("shadowitem") != null) {
            getCommand("shadowitem").setExecutor(
                    new ShadowCommand(shadowManager)
            );

            getLogger().info("Comando /shadowitem registrado.");
        } else {
            getLogger().severe(
                    "El comando shadowitem no está declarado en plugin.yml"
            );
        }

        getLogger().info("MiniHudBridge iniciado correctamente.");
    }

    @Override
    public void onDisable() {
        if (structuresHandler != null) {
            structuresHandler.unregister();
        }

        if (shadowManager != null) {
            shadowManager.save();
        }

        getLogger().info("MiniHudBridge detenido.");
    }

    public ServuxStructuresHandler getStructuresHandler() {
        return structuresHandler;
    }

    public ShadowManager getShadowManager() {
        return shadowManager;
    }
}