
package verymc.top.shadow;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

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

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    ChatColor.RED
                            + "Este comando solo puede ser usado por un jugador."
            );
            return true;
        }

        /*
         * Comando sin argumentos.
         */
        if (args.length != 1) {
            player.sendMessage(
                    ChatColor.YELLOW
                            + "Uso: /shadowitem <slot 1-9>"
            );

            player.sendMessage(
                    ChatColor.YELLOW
                            + "       /shadowitem remove"
            );

            return true;
        }

        /*
         * =========================================================
         * REMOVE
         * =========================================================
         *
         * Elimina TODAS las sincronizaciones Shadow.
         *
         * Los ítems que estén actualmente en inventarios,
         * cofres, hoppers, etc. NO se eliminan.
         *
         * Solamente se elimina la relación de sincronización.
         */
        if (args[0].equalsIgnoreCase("remove")) {

            int removed =
                    shadowManager.removeAllGroups();

            if (removed == 0) {
                player.sendMessage(
                        ChatColor.YELLOW
                                + "No había sincronizaciones Shadow."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.GREEN
                            + "Se eliminaron "
                            + removed
                            + " sincronizaciones Shadow."
            );

            player.sendMessage(
                    ChatColor.GRAY
                            + "Los ítems físicos no fueron modificados."
            );

            return true;
        }

        /*
         * =========================================================
         * SHADOW <SLOT>
         * =========================================================
         */

        int hotbarSlot;

        try {
            hotbarSlot =
                    Integer.parseInt(args[0]);

        } catch (NumberFormatException e) {

            player.sendMessage(
                    ChatColor.RED
                            + "El slot debe ser un número del 1 al 9."
            );

            return true;
        }

        /*
         * El jugador ve la hotbar como 1-9.
         *
         * NMS utiliza 0-8.
         *
         * Ejemplo:
         *
         * /shadowitem 1 -> NMS 0
         * /shadowitem 9 -> NMS 8
         */
        if (hotbarSlot < 1 || hotbarSlot > 9) {

            player.sendMessage(
                    ChatColor.RED
                            + "El slot debe estar entre 1 y 9."
            );

            return true;
        }

        int originalSlot =
                hotbarSlot - 1;

        /*
         * Obtenemos el jugador NMS.
         */
        CraftPlayer craftPlayer =
                (CraftPlayer) player;

        net.minecraft.server.level.ServerPlayer serverPlayer =
                craftPlayer.getHandle();

        Inventory inventory =
                serverPlayer.getInventory();

        /*
         * Obtenemos el ItemStack original.
         */
        ItemStack originalStack =
                inventory.getItem(originalSlot);

        if (
                originalStack == null
                        || originalStack.isEmpty()
        ) {

            player.sendMessage(
                    ChatColor.RED
                            + "El slot "
                            + hotbarSlot
                            + " de tu hotbar está vacío."
            );

            return true;
        }

        /*
         * Comprobamos si el slot original
         * ya pertenece a un Shadow.
         */
        if (
                shadowManager.findPlayerShadow(
                        player.getUniqueId(),
                        originalSlot
                ) != null
        ) {

            player.sendMessage(
                    ChatColor.RED
                            + "Ese slot ya pertenece a un Shadow."
            );

            return true;
        }

        /*
         * =========================================================
         * BUSCAR SEGUNDO SLOT
         * =========================================================
         *
         * Buscamos primero en toda la hotbar y después
         * en el inventario principal.
         */
        int secondSlot = -1;

        for (int i = 0; i < 36; i++) {

            /*
             * No podemos utilizar el mismo slot.
             */
            if (i == originalSlot) {
                continue;
            }

            /*
             * No usamos un slot que ya pertenezca
             * a otro Shadow.
             */
            if (
                    shadowManager.findPlayerShadow(
                            player.getUniqueId(),
                            i
                    ) != null
            ) {
                continue;
            }

            ItemStack candidate =
                    inventory.getItem(i);

            /*
             * Encontramos un slot vacío.
             */
            if (
                    candidate == null
                            || candidate.isEmpty()
            ) {

                secondSlot = i;

                break;
            }
        }

        /*
         * No hay espacio.
         */
        if (secondSlot == -1) {

            player.sendMessage(
                    ChatColor.RED
                            + "Necesitas al menos un slot vacío "
                            + "en tu inventario."
            );

            return true;
        }

        /*
         * =========================================================
         * CREAR ITEM SHADOW
         * =========================================================
         *
         * Creamos UNA sola instancia nueva.
         *
         * Los dos slots recibirán exactamente
         * la misma referencia NMS.
         */
        ItemStack shadowStack =
                originalStack.copy();

        /*
         * Creamos el grupo.
         */
        ShadowGroup group =
                shadowManager.createGroup();

        /*
         * =========================================================
         * ASIGNAR LA MISMA REFERENCIA
         * =========================================================
         */
        inventory.setItem(
                originalSlot,
                shadowStack
        );

        inventory.setItem(
                secondSlot,
                shadowStack
        );

        /*
         * =========================================================
         * REGISTRAR ENDPOINTS
         * =========================================================
         */
        group.addEndpoint(
                ShadowEndpoint.player(
                        player.getUniqueId(),
                        originalSlot
                )
        );

        group.addEndpoint(
                ShadowEndpoint.player(
                        player.getUniqueId(),
                        secondSlot
                )
        );

        /*
         * Guardamos el ItemStack maestro.
         */
        group.setShadowStack(
                shadowStack
        );

        /*
         * Guardamos shadows.yml.
         */
        shadowManager.save();

        /*
         * =========================================================
         * VERIFICACIÓN DE REFERENCIAS
         * =========================================================
         */
        ItemStack first =
                inventory.getItem(originalSlot);

        ItemStack second =
                inventory.getItem(secondSlot);

        int masterIdentity =
                System.identityHashCode(
                        shadowStack
                );

        int firstIdentity =
                System.identityHashCode(
                        first
                );

        int secondIdentity =
                System.identityHashCode(
                        second
                );

        boolean sameInstance =
                first == second
                        && first == shadowStack;

        /*
         * =========================================================
         * MENSAJES
         * =========================================================
         */
        player.sendMessage(
                ChatColor.GREEN
                        + "Shadow creado correctamente."
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Slot original: "
                        + ChatColor.WHITE
                        + hotbarSlot
                        + ChatColor.GRAY
                        + " de la hotbar."
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Segundo slot: "
                        + ChatColor.WHITE
                        + getDisplaySlot(secondSlot)
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Item: "
                        + ChatColor.WHITE
                        + shadowStack
                        .getHoverName()
                        .getString()
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Cantidad: "
                        + ChatColor.WHITE
                        + shadowStack.getCount()
        );

        /*
         * Debug de identidad.
         */
        player.sendMessage(
                ChatColor.DARK_GRAY
                        + "[DEBUG] Master="
                        + masterIdentity
                        + " | Original="
                        + firstIdentity
                        + " | Segundo="
                        + secondIdentity
        );

        player.sendMessage(
                sameInstance
                        ? ChatColor.GREEN
                                + "[DEBUG] MISMA INSTANCIA NMS ✓"
                        : ChatColor.RED
                                + "[DEBUG] INSTANCIAS DIFERENTES ✗"
        );

        return true;
    }

    /**
     * Convierte un índice NMS 0-35
     * a una descripción entendible para el jugador.
     */
    private String getDisplaySlot(int nmsSlot) {

        if (nmsSlot >= 0 && nmsSlot <= 8) {

            return "Hotbar "
                    + (nmsSlot + 1);
        }

        return "Inventario "
                + nmsSlot;
    }
}

