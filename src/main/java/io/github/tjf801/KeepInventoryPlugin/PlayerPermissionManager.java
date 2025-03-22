package io.github.tjf801.KeepInventoryPlugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.UUID;

public class PlayerPermissionManager implements Listener {
    private final KeepInventoryPlugin plugin;

    // contains a map of online players and their permission attachments
    private final HashMap<UUID, PermissionAttachment> keepInventory = new HashMap<>();

    public PlayerPermissionManager(KeepInventoryPlugin plugin) {this.plugin = plugin;}

    public PermissionAttachment getAttachment(@Nonnull Player player) {
        UUID uuid = player.getUniqueId();

        if (keepInventory.containsKey(uuid))
            return keepInventory.get(uuid);

        var attachment = player.addAttachment(plugin);
        keepInventory.put(uuid, attachment);
        return attachment;
    }

    void setKeepInventory(@Nonnull Player player, boolean enabled) {
        this.keepInventory.get(player.getUniqueId()).setPermission("keepinventory.enabled", enabled);
        updatePlayerConfigs(player);
    }

    void applyDefaultPermissions(@Nonnull Player player, @Nonnull FileConfiguration config) {
        var attachment = getAttachment(player);

        // remove all permissions (except keepinventory.enabled)
        attachment.getPermissions().keySet().stream()
                .filter(permission -> !permission.equals("keepinventory.enabled"))
                .forEach(attachment::unsetPermission);

        // apply default permissions
        config.getStringList("keepinventory.default-permissions")
                .forEach(permission -> attachment.setPermission(permission, true));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var config = plugin.getConfig();

        // apply default permissions
        applyDefaultPermissions(player, config);

        // if the user is joining for the first time and keepInventory is
        // enabled by default, add them to the config file
        if (!player.hasPlayedBefore() && config.getBoolean("keepinventory.enabled-by-default")) {
            // add the player to the config
            config.getStringList("keepinventory.enabled-player-uuids")
                    .add(player.getUniqueId().toString());

            // save the config file just in case the server crashes for some reason
            plugin.saveConfig();
        }

        // if the user is in the config, give them keepInventory
        getAttachment(player).setPermission(
                "keepinventory.enabled",
                config.getStringList("keepinventory.enabled-player-uuids")
                        .contains(player.getUniqueId().toString())
        );
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        updatePlayerConfigs(event.getPlayer());

        // remove the permission attachment to prevent memory leaks
        UUID playerUUID = event.getPlayer().getUniqueId();
        event.getPlayer().removeAttachment(keepInventory.get(playerUUID));
        keepInventory.remove(playerUUID);
    }

    public void updatePlayerConfigs(Player... players) {
        var config = plugin.getConfig();
        var enabledPlayerUUIDs = config.getStringList("keepinventory.enabled-player-uuids");

        for (var player : players) {
            String uuidString = player.getUniqueId().toString();
            boolean hasKeepInventory = player.hasPermission("keepinventory.enabled");

            // if the user has keepInventory, add them to the config if necessary
            if (hasKeepInventory && !enabledPlayerUUIDs.contains(uuidString))
                enabledPlayerUUIDs.add(uuidString);

            // NOTE: you don't have to check if it's in the list
            //       here because remove() is a no-op if it's not
            else if (!hasKeepInventory) enabledPlayerUUIDs.remove(uuidString);
        }

        // save the config file
        config.set("keepinventory.enabled-player-uuids", enabledPlayerUUIDs);
        plugin.saveConfig();
    }
}
