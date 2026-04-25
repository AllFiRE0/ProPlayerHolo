package com.allfire.proplayerholo;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class PlayerDisplayManager {
    private final Map<UUID, DisplayHologram> activeDisplays = new HashMap<>();
    private final Map<UUID, UUID> viewerToTarget = new HashMap<>();
    private final ProPlayerHolo plugin;
    private final ConfigManager configManager;
    private final PlayerProfileManager profileManager;

    public PlayerDisplayManager(ProPlayerHolo plugin, ConfigManager configManager, PlayerProfileManager profileManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.profileManager = profileManager;
        startUpdateTask();
    }

    public void showDisplay(Player viewer, Player target) {
        removeDisplay(viewer);

        ConfigManager.ProfileConfig profile = profileManager.getProfileConfig(viewer);
        DisplayHologram hologram = new DisplayHologram(target, viewer, plugin, profile);
        hologram.create();

        activeDisplays.put(target.getUniqueId(), hologram);
        viewerToTarget.put(viewer.getUniqueId(), target.getUniqueId());
    }

    public void removeDisplay(Player viewer) {
        UUID targetId = viewerToTarget.remove(viewer.getUniqueId());
        if (targetId != null) {
            DisplayHologram hologram = activeDisplays.remove(targetId);
            if (hologram != null) {
                hologram.remove();
            }
        }
    }

    public boolean hasActiveDisplay(Player viewer) {
        return viewerToTarget.containsKey(viewer.getUniqueId());
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, UUID> entry : new HashMap<>(viewerToTarget).entrySet()) {
                    Player viewer = plugin.getServer().getPlayer(entry.getKey());
                    Player target = plugin.getServer().getPlayer(entry.getValue());

                    if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
                        if (viewer != null) {
                            removeDisplay(viewer);
                        } else {
                            // Если viewer оффлайн, чистим по target
                            DisplayHologram hologram = activeDisplays.remove(entry.getValue());
                            if (hologram != null) hologram.remove();
                            viewerToTarget.remove(entry.getKey());
                        }
                        continue;
                    }

                    // Проверка дистанции между viewer и target
                    if (viewer.getLocation().distance(target.getLocation()) > configManager.getMaxDistance()) {
                        removeDisplay(viewer);
                        continue;
                    }

                    // Проверка что viewer всё ещё смотрит на target
                    if (!isLookingAt(viewer, target)) {
                        removeDisplay(viewer);
                        continue;
                    }

                    DisplayHologram hologram = activeDisplays.get(target.getUniqueId());
                    if (hologram != null) {
                        hologram.updatePosition();
                        hologram.updateText();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, configManager.getUpdateInterval());
    }

    private boolean isLookingAt(Player viewer, Player target) {
        Location eye = viewer.getEyeLocation();
        Vector direction = eye.getDirection();
        Vector toTarget = target.getLocation().add(0, 1, 0).toVector().subtract(eye.toVector());

        double dot = direction.normalize().dot(toTarget.normalize());
        return dot > configManager.getViewAngle();
    }

    public void cleanup() {
        activeDisplays.values().forEach(DisplayHologram::remove);
        activeDisplays.clear();
        viewerToTarget.clear();
    }
}
