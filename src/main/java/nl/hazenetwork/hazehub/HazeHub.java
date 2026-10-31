
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

    // Vijfde slot in de hotbar
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
    }

    // Controleert of een item het HazeHub-kompas is
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

    // Maakt het speciale serverselector-kompas
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

    // Geeft het kompas in slot 5
    private void give(Player player) {

        // Verwijder oude HazeHub-kompassen
        for (int i = 0; i < player.getInventory().getSize(); i++) {

            ItemStack item = player.getInventory().getItem(i);

            if (isSelector(item)) {
                player.getInventory().setItem(i, null);
            }
        }

        // Controleer of slot 5 bezet is
        ItemStack existing = player.getInventory().getItem(SLOT);

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

        // Plaats kompas in slot 5
        player.getInventory().setItem(
            SLOT,
            makeSelector()
        );
    }

    // Kompas geven bij joinen
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

    // Kompas teruggeven na respawn
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

    // Rechtsklik op kompas opent /servers
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        // Alleen hoofdhand
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // Alleen het HazeHub-kompas
        if (!isSelector(event.getItem())) {
            return;
        }

        // Alleen rechtsklikken
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();

        // Open DeluxeMenus op de volgende tick
        Bukkit.getScheduler().runTask(
            this,
            () -> {

                if (player.isOnline()) {
                    player.performCommand("servers");
                }

            }
        );
    }

    // Voorkom weggooien van het kompas
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

        // Voorkom hotbar-swaps met slot 5
        if (event.getHotbarButton() == SLOT) {
            event.setCancelled(true);
            return;
        }

        // Voorkom klikken op slot 5
        if (event.getClickedInventory() ==
            event.getWhoClicked().getInventory() &&
            event.getSlot() == SLOT) {

            event.setCancelled(true);
        }
    }

    // Voorkom verplaatsen via slepen
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

    // Voorkom kompas naar offhand verplaatsen
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {

        if (isSelector(event.getMainHandItem()) ||
            isSelector(event.getOffHandItem())) {

            event.setCancelled(true);
        }
    }
}
