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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

public final class HazeHub extends JavaPlugin implements Listener {
    private NamespacedKey key;
    private static final int SLOT = 4;

    @Override public void onEnable() {
        key = new NamespacedKey(this, "serverselector");
        Bukkit.getPluginManager().registerEvents(this, this);
        for (Player player : Bukkit.getOnlinePlayers()) give(player);
        getLogger().info("HazeHub enabled: compass in hotbar slot 5");
    }

    private boolean isSelector(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    @SuppressWarnings("deprecation")
    private ItemStack makeSelector() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§lHazeNetwork §8| §fServerselector");
        meta.setLore(List.of("§7Rechtsklik om een server te kiezen"));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return item;
    }

    private void give(Player player) {
        // Only on the dedicated lobby: remove previously issued selector items.
        for (int i=0; i<player.getInventory().getSize(); i++) {
            if (isSelector(player.getInventory().getItem(i))) player.getInventory().setItem(i, null);
        }
        ItemStack existing = player.getInventory().getItem(SLOT);
        if (existing != null && existing.getType() != Material.AIR) {
            java.util.HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(existing.clone());
            if (!overflow.isEmpty()) {
                getLogger().warning("Inventory full for " + player.getName() + "; cannot place selector in slot 5 safely.");
                return;
            }
        }
        player.getInventory().setItem(SLOT, makeSelector());
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (event.getPlayer().isOnline()) give(event.getPlayer());
        }, 10L);
    }

    @EventHandler public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (event.getPlayer().isOnline()) give(event.getPlayer());
        }, 10L);
    }

    @EventHandler public void onInteract(PlayerInteractEvent event) {
        if (!isSelector(event.getItem())) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            event.getPlayer().performCommand("servers");
        }
    }

    @EventHandler public void onDrop(PlayerDropItemEvent event) {
        if (isSelector(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (isSelector(event.getCurrentItem()) || isSelector(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        // Prevent swapping a different item into the protected hotbar slot.
        if (event.getHotbarButton() == SLOT) event.setCancelled(true);
        if (event.getClickedInventory() == event.getWhoClicked().getInventory() && event.getSlot() == SLOT) event.setCancelled(true);
    }

    @EventHandler public void onDrag(InventoryDragEvent event) {
        if (event.getRawSlots().contains(SLOT + event.getView().getTopInventory().getSize())) event.setCancelled(true);
    }
}
