package verymc.top.minihudbridge.shadow;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ShadowCommand implements CommandExecutor {

    private final ShadowManager shadowManager;

    public ShadowCommand(ShadowManager shadowManager) {
        this.shadowManager = shadowManager;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        // Solo jugadores
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    ChatColor.RED +
                    "Este comando solo puede ejecutarlo un jugador."
            );
            return true;
        }

        // /shadowitem <slot>
        if (args.length != 1) {
            player.sendMessage(
                    ChatColor.YELLOW +
                    "Uso: /shadowitem <1-9>"
            );
            return true;
        }

        int slot;

        try {
            slot = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(
                    ChatColor.RED +
                    "El slot debe ser un número del 1 al 9."
            );
            return true;
        }

        // El usuario escribe 1-9.
        // Bukkit internamente usa 0-8 para el hotbar.
        int inventorySlot = slot - 1;

        if (inventorySlot < 0 || inventorySlot > 8) {
            player.sendMessage(
                    ChatColor.RED +
                    "El slot debe estar entre 1 y 9."
            );
            return true;
        }

        ItemStack item =
                player.getInventory().getItem(inventorySlot);

        // Slot vacío
        if (item == null || item.isEmpty()) {
            player.sendMessage(
                    ChatColor.RED +
                    "Ese slot está vacío."
            );
            return true;
        }

        // Ya existe un Shadow para ese slot
        ShadowGroup existing =
                shadowManager.findPlayerShadow(
                        player.getUniqueId(),
                        inventorySlot
                );

        if (existing != null) {
            player.sendMessage(
                    ChatColor.YELLOW +
                    "Ese slot ya tiene un ShadowItem activo."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                    "Grupo: " +
                    existing.getId()
            );

            return true;
        }

        // Crear grupo
        ShadowGroup group =
                shadowManager.createGroup();

        // Registrar el slot del jugador
        group.addEndpoint(
                ShadowEndpoint.player(
                        player.getUniqueId(),
                        inventorySlot
                )
        );

        // Guardar
        shadowManager.save();

        player.sendMessage(
                ChatColor.GREEN +
                "ShadowItem registrado."
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Slot: " +
                slot
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Item: " +
                item.getType().name()
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Grupo: " +
                group.getId()
        );

        return true;
    }
}