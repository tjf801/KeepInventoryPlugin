package io.github.tjf801.KeepInventoryPlugin;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

// TODO: refactor the whole plugin (i made this in like 4 hours at 2AM, and i'm not a java dev at ALL lol)
//       - add a method for giving a specified player all the necessary permissions
//       - maybe factor out all the event handlers + keepInventory hashmap into a separate class
//       - add a KeepInventoryPlugin::updateConfig() method for updating the config file
public class KeepInventoryPlugin extends JavaPlugin implements Listener {
    // TODO: should this be sync?
    // contains a map of online players and their permission attachments
    private final HashMap<UUID, PermissionAttachment> keepInventory = new HashMap<>();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // if the user has keepInventory, don't drop their items or xp
        if (event.getEntity().hasPermission("keepinventory.enabled")) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // give the joining user keepInventory if they are in config.yml or if it's
        // their first time joining the server and keepInventory is enabled by default
        // (also give them the default permissions)

        // NOTE: we don't have to check if the user is in the keepInventory map because
        //       they are just joining now, so they can't be in the map (its for online players only)
        PermissionAttachment attachment = event.getPlayer().addAttachment(this);

        // apply default permissions
        for (String permission : getConfig().getStringList("keepinventory.default-permissions"))
            attachment.setPermission(permission, true);

        var playerUUID = event.getPlayer().getUniqueId();

        // check if the user is in the config
        boolean enabled = getConfig()
                .getStringList("keepinventory.enabled-player-uuids")
                .contains(playerUUID.toString());

        attachment.setPermission(
                "keepinventory.enabled",
                enabled || (
                        event.getPlayer().hasPlayedBefore()
                        && getConfig().getBoolean("keepinventory.enabled-by-default")
                )
        );

        keepInventory.put(playerUUID, attachment);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        // get the user's permission attachment
        // update+save the config file with the current value
        // remove the permission attachment from the hashmap
        var playerUUID = event.getPlayer().getUniqueId();

        // update the config
        var config = getConfig();
        var enabledPlayerUUIDs = config.getStringList("keepinventory.enabled-player-uuids");
        if (event.getPlayer().hasPermission("keepinventory.enabled")) {
            if (!enabledPlayerUUIDs.contains(playerUUID.toString())) {
                enabledPlayerUUIDs.add(playerUUID.toString());
            }
        } else {
            enabledPlayerUUIDs.remove(playerUUID.toString());
        }
        config.set("keepinventory.enabled-player-uuids", enabledPlayerUUIDs);
        saveConfig();

        // remove the permission attachment
        event.getPlayer().removeAttachment(keepInventory.get(playerUUID));
        keepInventory.remove(playerUUID);
    }

    @Override
    public void onEnable() {
        // set up the command tree
        this.registerCommand();

        // saves a copy of the default config.yml if one is not there
        this.saveDefaultConfig();

        // get all default permissions
        var defaultPermissions = getConfig().getStringList("keepinventory.default-permissions");

        // give all online players their default permissions
        for (Player player : Bukkit.getOnlinePlayers()) {
            PermissionAttachment attachment = player.addAttachment(this);

            // apply default permissions
            for (String permission : defaultPermissions) attachment.setPermission(permission, true);

            // if the user is in the config, give them keepInventory
            if (getConfig()
                    .getStringList("keepinventory.enabled-player-uuids")
                    .contains(player.getUniqueId().toString()))
            {
                attachment.setPermission("keepinventory.enabled", true);
            }

            keepInventory.put(player.getUniqueId(), attachment);
        }

        // register all events
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("KeepInventory v1.0 has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        // update the config with the current values
        var config = getConfig();
        var enabledPlayerUUIDs = config.getStringList("keepinventory.enabled-player-uuids");
        for (var player : Bukkit.getOnlinePlayers()) {
            var playerUUID = player.getUniqueId();
            if (player.hasPermission("keepinventory.enabled")) {
                if (!enabledPlayerUUIDs.contains(playerUUID.toString())) {
                    enabledPlayerUUIDs.add(playerUUID.toString());
                }
            } else {
                enabledPlayerUUIDs.remove(playerUUID.toString());
            }
        }
        config.set("keepinventory.enabled-player-uuids", enabledPlayerUUIDs);

        // save the config to file
        saveConfig();

        // remove all permission attachments (to avoid memory leaks)
        Collection<PermissionAttachment> attachments = keepInventory.values();
        for (PermissionAttachment attachment : attachments) attachment.remove();
        keepInventory.clear();

        getLogger().info("KeepInventory v1.0 has been successfully disabled");
    }

    void registerCommand() {
        // syntax: keepinventory <on|off> [targets]
        // TODO: add a way to check if keepInventory is enabled for a player
        new CommandTree("keepinventory")
                .then( new LiteralArgument("off").withPermission("keepinventory.command.self.off")
                        .executesPlayer((player, args) -> {
                            // turn off keepinventory for the player
                            keepInventory.get(player.getUniqueId()).setPermission("keepinventory.enabled", false);
                            player.sendMessage("keepInventory is now disabled.");
                        })
                        .then( new EntitySelectorArgument.ManyPlayers("targets").withPermission("keepinventory.command")
                                .executes((sender, args) -> {
                                    // turn off keepinventory for the targets
                                    for (var target : (Collection<Player>) args[0]) {
                                        keepInventory.get(target.getUniqueId()).setPermission("keepinventory.enabled", false);
                                        sender.sendMessage("disabled keepInventory for " + target.getName());
                                    }
                                })))
                .then( new LiteralArgument("on").withPermission("keepinventory.command.self.on")
                        .executesPlayer((player, args) -> {
                            // turn on keepinventory for the player
                            keepInventory.get(player.getUniqueId()).setPermission("keepinventory.enabled", true);
                            player.sendMessage("keepInventory is now enabled!");
                        })
                        .then( new EntitySelectorArgument.ManyPlayers("targets").withPermission("keepinventory.command")
                                .executes((sender, args) -> {
                                    // turn on keepinventory for the targets
                                    for (var target : (Collection<Player>) args[0]) {
                                        keepInventory.get(target.getUniqueId()).setPermission("keepinventory.enabled", true);
                                        sender.sendMessage("enabled keepInventory for " + target.getName());
                                    }
                                })))
                .register();
    }
}
