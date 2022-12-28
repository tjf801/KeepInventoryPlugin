package io.github.tjf801.KeepInventoryPlugin;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class KeepInventoryPlugin extends JavaPlugin {
    DeathEventHandler deathHandler = new DeathEventHandler();
    PlayerPermissionManager playerPermissionManager = new PlayerPermissionManager(this);

    @Override
    public void onEnable() {
        // set up the command tree
        this.registerCommand();

        // saves a copy of the default config.yml if one is not there
        this.saveDefaultConfig();

        // give all online players their permissions
        // TODO: should this be moved to the PlayerPermissionManager constructor?
        var config = getConfig();
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerPermissionManager.applyDefaultPermissions(player, config);

            // TODO: add keepinventory to players who had it enabled already
            if (player.hasPermission("keepinventory.enabled"))
                getLogger().warning("TODO: give " + player.getName() + " keepInventory");
        }

        // register all events
        getServer().getPluginManager().registerEvents(deathHandler, this);
        getServer().getPluginManager().registerEvents(playerPermissionManager, this);

        getLogger().info("KeepInventory v"
                + getDescription().getVersion()
                + " has been successfully enabled!"
        );
    }

    @Override
    public void onDisable() {
        // get all online players and save their config values
        var onlinePlayers = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        playerPermissionManager.updatePlayerConfigs(onlinePlayers);

        getLogger().info("KeepInventory v"
                + getDescription().getVersion()
                + " has been successfully disabled"
        );
    }

    void registerCommand() {
        // TODO: refactor this absolute tumor of a command tree
        new CommandTree("keepinventory")
                .withHelp(
                        "enables/disables keepInventory for the specified player",
                        "full syntax: keepinventory [on|off|toggle] [targets]"
                )
                .withPermission("keepinventory.command.check.self")
                .executesPlayer((player, args) -> {
                    // tell the user if they have keepInventory enabled or not
                    player.sendMessage(
                            player.getName() + " keepInventory: " + player.hasPermission("keepinventory.enabled")
                    );
                })
                .then( new LiteralArgument("off")
                        .withPermission("keepinventory.command.self.off")
                        .executesPlayer((player, args) -> {
                            // turn off keepinventory for the player
                            playerPermissionManager.keepInventory.get(player.getUniqueId()).setPermission("keepinventory.enabled", false);
                            player.sendMessage("keepInventory is now disabled.");
                        })
                        .then( new EntitySelectorArgument.ManyPlayers("targets").withPermission("keepinventory.command.players")
                                .executes((sender, args) -> {
                                    // turn off keepinventory for the targets
                                    @SuppressWarnings("unchecked cast")
                                    Collection<Player> targets = (Collection<Player>) args[0];

                                    for (var target : targets) {
                                        playerPermissionManager.keepInventory.get(target.getUniqueId()).setPermission("keepinventory.enabled", false);
                                        sender.sendMessage("disabled keepInventory for " + target.getName());
                                    }
                                })))
                .then( new LiteralArgument("on").withPermission("keepinventory.command.self.on")
                        .executesPlayer((player, args) -> {
                            // turn on keepinventory for the player
                            playerPermissionManager.keepInventory.get(player.getUniqueId()).setPermission("keepinventory.enabled", true);
                            player.sendMessage("keepInventory is now enabled!");
                        })
                        .then( new EntitySelectorArgument.ManyPlayers("targets").withPermission("keepinventory.command.players")
                                .executes((sender, args) -> {
                                    // turn on keepinventory for the targets
                                    @SuppressWarnings("unchecked cast")
                                    Collection<Player> targets = (Collection<Player>) args[0];

                                    for (var target : targets) {
                                        playerPermissionManager.keepInventory.get(target.getUniqueId()).setPermission("keepinventory.enabled", true);
                                        sender.sendMessage("enabled keepInventory for " + target.getName());
                                    }
                                })))
                .then( new LiteralArgument("toggle").withPermission("keepinventory.command.self.toggle")
                        .executesPlayer((player, args) -> {
                            // toggle keepinventory for the player
                            var attachment = playerPermissionManager.keepInventory.get(player.getUniqueId());
                            attachment.setPermission("keepinventory.enabled",
                                    !player.hasPermission("keepinventory.enabled"));
                            player.sendMessage(
                                    "keepInventory for " + player.getName() + " is now "
                                    + player.hasPermission("keepinventory.enabled")
                            );
                        })
                        .then( new EntitySelectorArgument.ManyPlayers("targets").withPermission("keepinventory.command.players")
                                .executes((sender, args) -> {
                                    // toggle keepinventory for the targets
                                    @SuppressWarnings("unchecked cast")
                                    Collection<Player> targets = (Collection<Player>) args[0];

                                    for (var target : targets) {
                                        var attachment = playerPermissionManager.keepInventory.get(target.getUniqueId());
                                        attachment.setPermission("keepinventory.enabled",
                                                !target.hasPermission("keepinventory.enabled"));
                                        sender.sendMessage(
                                                "keepInventory for " + target.getName() + " is now "
                                                + target.hasPermission("keepinventory.enabled")
                                        );
                                    }
                                })))
                .register();
    }
}
