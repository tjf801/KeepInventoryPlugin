package io.github.tjf801.KeepInventoryPlugin;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class KeepInventoryPlugin extends JavaPlugin {
    DeathEventHandler deathHandler = new DeathEventHandler(this);
    PlayerPermissionManager playerPermissionManager = new PlayerPermissionManager(this);

    @Override
    public void onEnable() {
        // saves a copy of the default config.yml if one is not there
        this.saveDefaultConfig();

        // set up the command tree
        this.registerCommand();

        // give all online players their permissions
        // TODO: should this be moved to the PlayerPermissionManager constructor?
        var config = getConfig();
        getLogger().info(config.getStringList("keepinventory.default-permissions").toString());
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
        // just in case it was changed
        this.reloadConfig();

        // get all online players and save their config values
        var onlinePlayers = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        playerPermissionManager.updatePlayerConfigs(onlinePlayers);
        saveConfig();

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
                        "full syntax: keepinventory [on|off|toggle|query] [targets]"
                )
                .withPermission("keepinventory.command.check.self")
                .executesPlayer((player, args) -> {
                    // tell the user if they have keepInventory enabled or not
                    player.sendMessage(
                            player.getName() + " keepInventory: " + player.hasPermission("keepinventory.enabled")
                    );
                })
                .then( new LiteralArgument("reload")
                        .withPermission("keepinventory.command")
                        .executes((sender, args) -> {
                            // reload the config
                            reloadConfig();

                            // get all online players and save their config values
                            var onlinePlayers = Bukkit.getOnlinePlayers().toArray(new Player[0]);
                            playerPermissionManager.updatePlayerConfigs(onlinePlayers);
                            saveConfig();

                            sender.sendMessage("KeepInventory config reloaded");
                        })
                )
                .then( new LiteralArgument("query")
                        .withPermission("keepinventory.command.check.self")
                        .executesPlayer((player, args) -> {
                            // query sender by default
                            player.sendMessage(
                                    player.getName() + " keepInventory: " + player.hasPermission("keepinventory.enabled")
                            );
                        })
                        .then( new EntitySelectorArgument.ManyPlayers("targets")
                                .withPermission("keepinventory.command.check.players")
                                .executes((sender, args) -> {
                                    // query keepinventory for the targets
                                    @SuppressWarnings("unchecked cast")
                                    Collection<Player> targets = (Collection<Player>) args.args()[0];

                                    for (var target : targets) {
                                        sender.sendMessage(
                                                target.getName() + " keepInventory: " + target.hasPermission("keepinventory.enabled")
                                        );
                                    }
                                })))
                .then( new LiteralArgument("off")
                        .withPermission("keepinventory.command.self.off")
                        .executesPlayer((player, args) -> {
                            // turn off keepinventory for the player
                            playerPermissionManager.setKeepInventory(player, true);
                            player.sendMessage("keepInventory is now disabled.");
                        })
                        .then( new EntitySelectorArgument.ManyPlayers("targets").withPermission("keepinventory.command.players")
                                .executes((sender, args) -> {
                                    // turn off keepinventory for the targets
                                    @SuppressWarnings("unchecked cast")
                                    Collection<Player> targets = (Collection<Player>) args.args()[0];

                                    for (var target : targets) {
                                        playerPermissionManager.setKeepInventory(target, false);
                                        sender.sendMessage("disabled keepInventory for " + target.getName());
                                    }
                                })))
                .then( new LiteralArgument("on")
                        .withPermission("keepinventory.command.self.on")
                        .executesPlayer((player, args) -> {
                            // turn on keepinventory for the player
                            playerPermissionManager.setKeepInventory(player, true);
                            player.sendMessage("keepInventory is now enabled!");
                        })
                        .then( new EntitySelectorArgument.ManyPlayers("targets")
                                .withPermission("keepinventory.command.players")
                                .executes((sender, args) -> {
                                    // turn on keepinventory for the targets
                                    @SuppressWarnings("unchecked cast")
                                    Collection<Player> targets = (Collection<Player>) args.args()[0];

                                    for (var target : targets) {
                                        playerPermissionManager.setKeepInventory(target, true);
                                        sender.sendMessage("enabled keepInventory for " + target.getName());
                                    }
                                })))
                .then( new LiteralArgument("toggle")
                        .withPermission("keepinventory.command.self.toggle")
                        .executesPlayer((player, args) -> {
                            playerPermissionManager.setKeepInventory(player, !player.hasPermission("keepinventory.enabled"));
                        })
                        .then( new EntitySelectorArgument.ManyPlayers("targets")
                                .withPermission("keepinventory.command.players")
                                .executes((sender, args) -> {
                                    // toggle keepinventory for the targets
                                    @SuppressWarnings("unchecked cast")
                                    Collection<Player> targets = (Collection<Player>) args.args()[0];

                                    for (var target : targets) {
                                        playerPermissionManager.setKeepInventory(target, !target.hasPermission("keepinventory.enabled"));
                                    }
                                })))
                .then(new LiteralArgument("pvp")
                        .withPermission("keepinventory.command.pvp")
                        .executes((sender, args) -> {
                            var curr = this.getConfig().getBoolean("keepinventory.pvp-keepinventory");
                            this.getConfig().set("keepinventory.pvp-keepinventory", !curr);
                            sender.sendMessage("PVP KeepInventory is now " + (!curr ? "en" : "dis") + "abled");
                        })
                )
                .register();
    }
}
