
package nl.hazenetwork.hazehub;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public final class HazeHub extends JavaPlugin implements Listener {

    private static final int SLOT = 4;
    private static final Set<String> SERVERS =
            Set.of("smp01", "hardcore", "minigames");

    private NamespacedKey key;

    @Override
    public void onEnable() {
        key = new NamespacedKey(this, "serverselector");

        Bukkit.getPluginManager().registerEvents(this, this);

        // Verbinding met Velocity via BungeeCord-kanaal
        getServer().getMessenger().registerOutgoingPluginChannel(
                this, "BungeeCord"
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            give(player);
        }

        getLogger().info("HazeHub gestart!");
        getLogger().info("Serverselector en NPC-verbindingen actief.");
    }

    private boolean isSelector(ItemStack item) {
        return item != null
                && item.getType() == Material.COMPASS
                && item.hasItemMeta()
                && item.getItemMeta()
                    .getPersistentDataContainer()
                    .has(key, PersistentDataType.BYTE);
    }

    @SuppressWarnings("deprecation")
    private ItemStack makeSelector() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(
                "§b§lHazeNetwork §8| §fServerselector"
        );

        meta.setLore(List.of(
                "§7Rechtsklik om een server te kiezen"
        ));

        meta.getPersistentDataContainer().set(
                key, PersistentDataType.BYTE, (byte) 1
        );

        item.setItemMeta(meta);
        return item;
    }

    private void give(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (isSelector(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, null);
            }
        }

        ItemStack existing = player.getInventory().getItem(SLOT);

        if (existing != null && existing.getType() != Material.AIR) {
            HashMap<Integer, ItemStack> overflow =
                    player.getInventory().addItem(existing.clone());

            if (!overflow.isEmpty()) {
                getLogger().warning(
                        "Inventaris vol voor " + player.getName()
                );
                return;
            }

            player.getInventory().setItem(SLOT, null);
        }

        player.getInventory().setItem(SLOT, makeSelector());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (event.getPlayer().isOnline()) {
                give(event.getPlayer());
            }
        }, 10L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (event.getPlayer().isOnline()) {
                give(event.getPlayer());
            }
        }, 10L);
    }

    // Kompas opent DeluxeMenus
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!isSelector(event.getItem())) return;

        event.setCancelled(true);

        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        "dm open serverselector " + player.getName()
                );
            }
        });
    }

    // /hazehub connect <server>
    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dit commando moet door een speler worden uitgevoerd.");
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("connect")) {
            player.sendMessage("§eGebruik: /hazehub connect <server>");
            return true;
        }

        String server = args[1].toLowerCase(java.util.Locale.ROOT);

        if (!SERVERS.contains(server)) {
            player.sendMessage("§cDeze server is niet beschikbaar.");
            return true;
        }

        connect(player, server);
        return true;
    }

    // Stuur speler via Velocity naar de gekozen server
    private void connect(Player player, String server) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            out.writeUTF("Connect");
            out.writeUTF(server);

            player.sendPluginMessage(
                    this, "BungeeCord", bytes.toByteArray()
            );

            player.sendMessage("§bHazeNetwork §8» §7Verbinden met §f" + server + "§7...");

        } catch (IOException exception) {
            getLogger().severe(
                    "Verbinding mislukt: " + exception.getMessage()
            );

            player.sendMessage("§cKan de serververbinding niet starten.");
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isSelector(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        if (isSelector(event.getCurrentItem())
                || isSelector(event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        if (event.getHotbarButton() == SLOT) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory()
                == event.getWhoClicked().getInventory()
                && event.getSlot() == SLOT) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        int rawSlot = SLOT
                + event.getView().getTopInventory().getSize();

        if (event.getRawSlots().contains(rawSlot)) {
            event.setCancelled(true);
            return;
        }

        for (ItemStack item : event.getNewItems().values()) {
            if (isSelector(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (isSelector(event.getMainHandItem())
                || isSelector(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }
}
