package com.allfire.proplayerholo;

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

        PluginCommand cmd = getCommand("proplayerholo");
        if (cmd != null) cmd.setTabCompleter(this);

        getLogger().info("ProPlayerHolo enabled! Author: AllF1RE");
    }

    @Override
    public void onDisable() {
        if (displayManager != null) displayManager.cleanup();
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
        if (removed > 0) getLogger().info("Cleaned " + removed + " orphan holograms");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (sender.hasPermission("proplayerholo.reload")) {
                    configManager.reload();
                    sendMsg(sender, configManager.getMessage("config-reloaded"));
                } else sendMsg(sender, configManager.getMessage("no-permission"));
            }
            case "profile" -> {
                if (sender instanceof Player p) handleProfile(p, args);
            }
            case "list" -> {
                if (sender instanceof Player p) listProfiles(p);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                                 @NotNull String label, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("reload"); list.add("profile"); list.add("list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("profile")) {
            list.addAll(configManager.getProfileIds());
        }
        return list;
    }

    private void handleProfile(Player player, String[] args) {
        if (args.length < 2) { listProfiles(player); return; }
        String id = args[1];
        ConfigManager.ProfileConfig profile = configManager.getProfile(id);
        if (profile == null) {
            sendMsg(player, configManager.getMessage("profile-not-found").replace("%profile%", id));
            return;
        }
        if (!player.hasPermission(profile.getPermission()) && !player.hasPermission("proplayerholo.admin")) {
            sendMsg(player, configManager.getMessage("no-profile-permission").replace("%profile%", id));
            return;
        }
        profileManager.setProfile(player, id);
        sendMsg(player, configManager.getMessage("profile-selected").replace("%profile_name%", profile.getName()));
    }

    private void listProfiles(Player player) {
        sendMsg(player, configManager.getMessage("profile-list-header"));
        for (ConfigManager.ProfileConfig p : configManager.getAllProfiles()) {
            if (player.hasPermission(p.getPermission()) || player.hasPermission("proplayerholo.admin")) {
                sendMsg(player, configManager.getMessage("profile-list-item")
                        .replace("%profile_id%", p.getId())
                        .replace("%profile_name%", p.getName())
                        .replace("%permission%", p.getPermission()));
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sendMsg(sender, "<gold>ProPlayerHolo v1.0.0 by <white>AllF1RE");
        sendMsg(sender, "<yellow>/pph reload <gray>- Reload config");
        sendMsg(sender, "<yellow>/pph profile <id> <gray>- Select profile");
        sendMsg(sender, "<yellow>/pph list <gray>- List profiles");
    }

    private void sendMsg(CommandSender sender, String msg) {
        if (msg != null && !msg.isEmpty()) sender.sendMessage(MiniMessage.miniMessage().deserialize(msg));
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player target)) return;
        if (!e.getPlayer().isSneaking()) return;
        Player viewer = e.getPlayer();

        if (!viewer.hasPermission("proplayerholo.use")) {
            if (!configManager.isSilentNoPermission()) sendMsg(viewer, configManager.getMessage("no-permission"));
            return;
        }
        e.setCancelled(true);

        if (displayManager.hasActiveDisplay(viewer)) {
            displayManager.removeDisplay(viewer);
        }
        displayManager.showDisplay(viewer, target);
        sendMsg(viewer, configManager.getMessage("display-on").replace("%player%", target.getName()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (displayManager.hasActiveDisplay(e.getPlayer())) displayManager.removeDisplay(e.getPlayer());
        profileManager.removePlayer(e.getPlayer());
    }
}
