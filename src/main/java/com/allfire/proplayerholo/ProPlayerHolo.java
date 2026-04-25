package com.allfire.proplayerholo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ProPlayerHolo extends JavaPlugin implements Listener, TabCompleter {
    private PlayerDisplayManager displayManager;
    private ConfigManager configManager;
    private PlayerProfileManager profileManager;

    @Override
    public void onEnable() {
        cleanOrphanDisplays();

        configManager = new ConfigManager(this);
        profileManager = new PlayerProfileManager(configManager);
        displayManager = new PlayerDisplayManager(this, configManager, profileManager);

        getServer().getPluginManager().registerEvents(this, this);
        
        PluginCommand command = getCommand("proplayerholo");
        if (command != null) {
            command.setTabCompleter(this);
        }
        
        getLogger().info("ProPlayerHolo enabled! Author: AllF1RE");
    }

    @Override
    public void onDisable() {
        if (displayManager != null) {
            displayManager.cleanup();
        }
        getLogger().info("ProPlayerHolo disabled!");
    }

    private void cleanOrphanDisplays() {
        int removed = 0;
        for (World world : getServer().getWorlds()) {
            for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
                if (display.getScoreboardTags().contains("pph_hologram")) {
                    display.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            getLogger().info("Cleaned up " + removed + " orphan holograms from previous session");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (sender.hasPermission("proplayerholo.reload")) {
                    configManager.reload();
                    sendMessage(sender, configManager.getMessage("config-reloaded"));
                } else {
                    sendMessage(sender, configManager.getMessage("no-permission"));
                }
                break;

            case "profile":
                if (sender instanceof Player player) {
                    handleProfileCommand(player, args);
                }
                break;

            case "list":
                if (sender instanceof Player player) {
                    listProfiles(player);
                }
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                 @NotNull Command command,
                                                 @NotNull String label,
                                                 @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("reload");
            completions.add("profile");
            completions.add("list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("profile")) {
            completions.addAll(configManager.getProfileIds());
        }
        
        return completions;
    }

    private void handleProfileCommand(Player player, String[] args) {
        if (args.length < 2) {
            listProfiles(player);
            return;
        }

        String profileId = args[1];
        ConfigManager.ProfileConfig profile = configManager.getProfile(profileId);

        if (profile == null) {
            sendMessage(player, configManager.getMessage("profile-not-found")
                    .replace("%profile%", profileId));
            return;
        }

        if (!player.hasPermission(profile.getPermission()) && !player.hasPermission("proplayerholo.admin")) {
            sendMessage(player, configManager.getMessage("no-profile-permission")
                    .replace("%profile%", profileId));
            return;
        }

        profileManager.setProfile(player, profileId);
        sendMessage(player, configManager.getMessage("profile-selected")
                .replace("%profile_name%", profile.getName()));
    }

    private void listProfiles(Player player) {
        sendMessage(player, configManager.getMessage("profile-list-header"));

        for (ConfigManager.ProfileConfig profile : configManager.getAllProfiles()) {
            if (player.hasPermission(profile.getPermission()) || player.hasPermission("proplayerholo.admin")) {
                String message = configManager.getMessage("profile-list-item")
                        .replace("%profile_id%", profile.getId())
                        .replace("%profile_name%", profile.getName())
                        .replace("%permission%", profile.getPermission());
                sendMessage(player, message);
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "<gold>ProPlayerHolo v1.0.0 by <white>AllF1RE");
        sendMessage(sender, "<yellow>/pph reload <gray>- Reload config");
        sendMessage(sender, "<yellow>/pph profile <id> <gray>- Select profile");
        sendMessage(sender, "<yellow>/pph list <gray>- List available profiles");
    }

    private void sendMessage(CommandSender sender, String message) {
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(message));
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;
        if (!event.getPlayer().isSneaking()) return;

        Player viewer = event.getPlayer();

        if (!viewer.hasPermission("proplayerholo.use")) {
            if (!configManager.isSilentNoPermission()) {
                sendMessage(viewer, configManager.getMessage("no-permission"));
            }
            return;
        }

        event.setCancelled(true);

        if (displayManager.hasActiveDisplay(viewer)) {
            displayManager.removeDisplay(viewer);
        }

        displayManager.showDisplay(viewer, target);
        sendMessage(viewer, configManager.getMessage("display-on")
                .replace("%player%", target.getName()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (displayManager.hasActiveDisplay(event.getPlayer())) {
            displayManager.removeDisplay(event.getPlayer());
        }
        profileManager.removePlayer(event.getPlayer());
    }
}
