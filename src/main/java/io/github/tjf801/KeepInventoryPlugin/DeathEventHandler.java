package io.github.tjf801.KeepInventoryPlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathEventHandler implements Listener {
    private final KeepInventoryPlugin plugin;

    public DeathEventHandler(KeepInventoryPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // check if pvp KE is enabled and this is a PVP death
        if (plugin.getConfig().getBoolean("keepinventory.pvp-keepinventory") && event.getEntity().getKiller() != null) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
            return;
        }

        // if the user has keepInventory, don't drop their items or xp
        if (event.getEntity().hasPermission("keepinventory.enabled")) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }
}
