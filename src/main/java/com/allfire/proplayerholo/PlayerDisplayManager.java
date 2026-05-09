package com.allfire.proplayerholo;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class PlayerDisplayManager {
    private final Map<UUID, DisplayHologram> activeDisplays = new HashMap<>();
    private final Map<UUID, UUID> viewerToTarget = new HashMap<>();
    private final Map<UUID, Long> viewerFaceTimers = new HashMap<>();
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

        if (profile.getMode() == ConfigManager.HologramMode.VIEWER_FACE) {
            viewerFaceTimers.put(viewer.getUniqueId(), System.currentTimeMillis() + profile.getDuration() * 1000L);
        }
    }

    public void removeDisplay(Player viewer) {
        UUID targetId = viewerToTarget.remove(viewer.getUniqueId());
        viewerFaceTimers.remove(viewer.getUniqueId());
        if (targetId != null) {
            DisplayHologram hologram = activeDisplays.remove(targetId);
            if (hologram != null) hologram.remove();
        }
    }

    public boolean hasActiveDisplay(Player viewer) {
        return viewerToTarget.containsKey(viewer.getUniqueId());
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (Map.Entry<UUID, UUID> entry : new HashMap<>(viewerToTarget).entrySet()) {
                    Player viewer = plugin.getServer().getPlayer(entry.getKey());
                    Player target = plugin.getServer().getPlayer(entry.getValue());

                    if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
                        if (viewer != null) removeDisplay(viewer);
                        else {
                            DisplayHologram h = activeDisplays.remove(entry.getValue());
                            if (h != null) h.remove();
                            viewerToTarget.remove(entry.getKey());
                            viewerFaceTimers.remove(entry.getKey());
                        }
                        continue;
                    }

                    DisplayHologram hologram = activeDisplays.get(target.getUniqueId());
                    if (hologram == null) continue;

                    ConfigManager.ProfileConfig profile = profileManager.getProfileConfig(viewer);
                    boolean isViewerFace = profile.getMode() == ConfigManager.HologramMode.VIEWER_FACE;

                    // ──── Для VIEWER_FACE ────
                    if (isViewerFace) {
                        // Проверка только таймера
                        Long expireTime = viewerFaceTimers.get(viewer.getUniqueId());
                        if (expireTime != null && now > expireTime) {
                            removeDisplay(viewer);
                            continue;
                        }
                        // Обновляем позицию (следует за головой viewer) и текст
                        hologram.updatePosition();
                        hologram.updateText();
                        continue;
                    }

                    // ──── Для TARGET_TRACKING ────
                    // Проверка дистанции
                    if (viewer.getLocation().distance(target.getLocation()) > configManager.getMaxDistance()) {
                        removeDisplay(viewer);
                        continue;
                    }

                    // Проверка взгляда с учётом разных ростов игроков
                    if (!isLookingAt(viewer, target)) {
                        removeDisplay(viewer);
                        continue;
                    }

                    hologram.updatePosition();
                    hologram.updateText();
                }
            }
        }.runTaskTimer(plugin, 0L, configManager.getUpdateInterval());
    }

    /**
     * Проверяет, смотрит ли viewer на target.
     * Учитывает разный рост игроков: проверяет взгляд на разные точки тела цели
     * (ноги, центр, голова, чуть выше головы).
     */
    private boolean isLookingAt(Player viewer, Player target) {
        Location eye = viewer.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        // Получаем рост цели
        double targetHeight = target.getHeight();

        // Проверяем несколько точек по вертикали цели:
        double[] checkHeights = {
                0.0,                    // ноги (для маленьких мобов/игроков)
                targetHeight * 0.5,     // центр тела
                targetHeight,           // голова
                targetHeight + 0.5      // чуть выше (запас)
        };

        double bestDot = -2.0;

        for (double heightOffset : checkHeights) {
            Location targetPoint = target.getLocation().clone().add(0, heightOffset, 0);
            Vector toTarget = targetPoint.toVector().subtract(eye.toVector());

            // Если цель слишком близко — не проверяем угол
            if (toTarget.length() < 0.5) return true;

            double dot = direction.dot(toTarget.normalize());
            if (dot > bestDot) bestDot = dot;
        }

        return bestDot > configManager.getViewAngle();
    }

    public void cleanup() {
        activeDisplays.values().forEach(DisplayHologram::remove);
        activeDisplays.clear();
        viewerToTarget.clear();
        viewerFaceTimers.clear();
    }
}
