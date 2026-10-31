
package nl.hazenetwork.hazehub;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

import java.util.HashMap;
import java.util.List;

public final class HazeHub extends JavaPlugin implements Listener {

    private NamespacedKey key;

    // Vijfde slot van de hotbar
    private static final int SLOT = 4;

    @Override
    public void onEnable() {

        key = new NamespacedKey(this, "serverselector");

        Bukkit.getPluginManager().registerEvents(this, this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            give(player);
        }

        getLogger().info("HazeHub ingeschakeld!");
        getLogger().info("Serverselector actief in hotbar slot 5.");
        getLogger().info("DEBUG-modus actief voor kompasinteractie.");
    }

    // Herkent het speciale HazeNetwork-kompas
    private boolean isSelector(ItemStack item) {

        if (item == null ||
            item.getType() != Material.COMPASS ||
            !item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(key, PersistentDataType.BYTE);
    }

    // Maakt het serverselector-kompas
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
            key,
            PersistentDataType.BYTE,
            (byte) 1
        );

        item.setItemMeta(meta);

        return item;
    }

    // Geeft het kompas in hotbar-slot 5
    private void give(Player player) {

        // Verwijder eerder uitgegeven kompassen
        for (int i = 0; i < player.getInventory().getSize(); i++) {

            ItemStack item = player.getInventory().getItem(i);

            if (isSelector(item)) {
                player.getInventory().setItem(i, null);
            }
        }

        ItemStack existing = player.getInventory().getItem(SLOT);

        // Verplaats een eventueel bestaand item
        if (existing != null &&
            existing.getType() != Material.AIR) {

            HashMap<Integer, ItemStack> overflow =
                player.getInventory().addItem(existing.clone());

            if (!overflow.isEmpty()) {

                getLogger().warning(
                    "Inventaris vol voor " + player.getName()
                );

                return;
            }
        }

        player.getInventory().setItem(
            SLOT,
            makeSelector()
        );
    }

    // Geef kompas bij joinen
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Bukkit.getScheduler().runTaskLater(
            this,
            () -> {

                if (event.getPlayer().isOnline()) {
                    give(event.getPlayer());
                }

            },
            10L
        );
    }

    // Geef kompas na respawn
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {

        Bukkit.getScheduler().runTaskLater(
            this,
            () -> {

                if (event.getPlayer().isOnline()) {
                    give(event.getPlayer());
                }

            },
            10L
        );
    }

    // DEBUG: controleer rechtsklik en open /servers
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        // Alleen hoofdhand
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // Alleen rechtsklikken
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        player.sendMessage(
            "§e[HazeHub TEST] Rechtsklik gedetecteerd!"
        );

        // Controleer of het kompas herkend wordt
        if (!isSelector(event.getItem())) {

            player.sendMessage(
                "§c[HazeHub TEST] Kompas niet herkend!"
            );

            return;
        }

        player.sendMessage(
            "§a[HazeHub TEST] Kompas herkend!"
        );

        event.setCancelled(true);

        // Voer DeluxeMenus-commando uit
        Bukkit.getScheduler().runTask(
            this,
            () -> {

                if (!player.isOnline()) {
                    return;
                }

                player.sendMessage(
                    "§e[HazeHub TEST] /servers uitvoeren..."
                );

                boolean success =
                    player.performCommand("servers");

                player.sendMessage(
                    "§e[HazeHub TEST] Commando uitgevoerd: "
                    + success
                );
            }
        );
    }

    // Voorkom weggooien
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {

        if (isSelector(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    // Voorkom verplaatsen via inventaris
    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (isSelector(event.getCurrentItem()) ||
            isSelector(event.getCursor())) {

            event.setCancelled(true);
            return;
        }

        // Voorkom verwisselen via hotbar
        if (event.getHotbarButton() == SLOT) {
            event.setCancelled(true);
            return;
        }

        // Bescherm slot 5
        if (event.getClickedInventory() ==
            event.getWhoClicked().getInventory() &&
            event.getSlot() == SLOT) {

            event.setCancelled(true);
        }
    }

    // Voorkom slepen
    @EventHandler
    public void onDrag(InventoryDragEvent event) {

        int rawSlot = SLOT +
            event.getView().getTopInventory().getSize();

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

    // Voorkom verplaatsen naar offhand
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {

        if (isSelector(event.getMainHandItem()) ||
            isSelector(event.getOffHandItem())) {

            event.setCancelled(true);
        }
    }
}
