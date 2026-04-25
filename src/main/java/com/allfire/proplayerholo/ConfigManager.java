package com.allfire.proplayerholo;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private final Map<String, ProfileConfig> profiles = new HashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        loadProfiles();
    }

    private void loadProfiles() {
        profiles.clear();
        ConfigurationSection profilesSection = config.getConfigurationSection("profiles");
        if (profilesSection != null) {
            for (String key : profilesSection.getKeys(false)) {
                ProfileConfig profile = new ProfileConfig(
                        key,
                        profilesSection.getString(key + ".permission"),
                        profilesSection.getString(key + ".name"),
                        profilesSection.getStringList(key + ".description"),
                        profilesSection.getDouble(key + ".display.size", 1.0),
                        profilesSection.getString(key + ".display.position", "ABOVE"),
                        profilesSection.getString(key + ".display.billboard", "VERTICAL"),
                        profilesSection.getDouble(key + ".display.height-offset", 0.5),
                        profilesSection.getDouble(key + ".display.offset-x", 0.0),
                        profilesSection.getDouble(key + ".display.offset-y", 0.0),
                        profilesSection.getDouble(key + ".display.offset-z", 0.0),
                        profilesSection.getBoolean(key + ".background.enabled", true),
                        profilesSection.getDouble(key + ".background.opacity", 0.3),
                        profilesSection.getString(key + ".background.color", "#000000"),
                        profilesSection.getBoolean(key + ".background.glowing", false),
                        profilesSection.getString(key + ".background.glow-color", "#FFFFFF"),
                        profilesSection.getStringList(key + ".lines")
                );
                profiles.put(key, profile);
            }
        }
    }

    public ProfileConfig getProfile(String id) {
        return profiles.get(id);
    }

    public Collection<ProfileConfig> getAllProfiles() {
        return profiles.values();
    }

    public Set<String> getProfileIds() {
        return profiles.keySet();
    }

    public String getDefaultProfile() {
        return config.getString("default-profile", "basic");
    }

    public double getMaxDistance() {
        return config.getDouble("settings.max-distance", 15.0);
    }

    public double getViewAngle() {
        return config.getDouble("settings.view-angle", 0.95);
    }

    public int getUpdateInterval() {
        return config.getInt("settings.update-interval", 10);
    }

    public boolean isSilentNoPermission() {
        return config.getBoolean("settings.silent-no-permission", true);
    }

    public String getMessage(String path) {
        return config.getString("messages." + path, "");
    }

    public enum HologramPosition {
        ABOVE,
        FRONT,
        BACK,
        LEFT,
        RIGHT,
        BELOW,
        ABOVE_FRONT
    }

    public enum BillboardMode {
        FIXED,      // Не поворачивается вообще
        VERTICAL,   // Поворачивается только по вертикали (смотрит на viewer)
        HORIZONTAL, // Поворачивается только по горизонтали
        CENTER      // Всегда лицом к viewer
    }

    public static class ProfileConfig {
        private final String id;
        private final String permission;
        private final String name;
        private final List<String> description;
        private final double size;
        private final HologramPosition position;
        private final BillboardMode billboard;
        private final double heightOffset;
        private final double offsetX;
        private final double offsetY;
        private final double offsetZ;
        private final boolean backgroundEnabled;
        private final double backgroundOpacity;
        private final String backgroundColor;
        private final boolean glowing;
        private final String glowColor;
        private final List<String> lines;

        public ProfileConfig(String id, String permission, String name, List<String> description,
                             double size, String position, String billboard, double heightOffset,
                             double offsetX, double offsetY, double offsetZ,
                             boolean backgroundEnabled, double backgroundOpacity,
                             String backgroundColor, boolean glowing, String glowColor,
                             List<String> lines) {
            this.id = id;
            this.permission = permission;
            this.name = name;
            this.description = description;
            this.size = size;
            this.position = parsePosition(position);
            this.billboard = parseBillboard(billboard);
            this.heightOffset = heightOffset;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.backgroundEnabled = backgroundEnabled;
            this.backgroundOpacity = backgroundOpacity;
            this.backgroundColor = backgroundColor;
            this.glowing = glowing;
            this.glowColor = glowColor;
            this.lines = lines;
        }

        private HologramPosition parsePosition(String pos) {
            try {
                return HologramPosition.valueOf(pos.toUpperCase());
            } catch (IllegalArgumentException e) {
                return HologramPosition.ABOVE;
            }
        }

        private BillboardMode parseBillboard(String mode) {
            try {
                return BillboardMode.valueOf(mode.toUpperCase());
            } catch (IllegalArgumentException e) {
                return BillboardMode.VERTICAL;
            }
        }

        public String getId() { return id; }
        public String getPermission() { return permission; }
        public String getName() { return name; }
        public List<String> getDescription() { return description; }
        public double getSize() { return size; }
        public HologramPosition getPosition() { return position; }
        public BillboardMode getBillboard() { return billboard; }
        public double getHeightOffset() { return heightOffset; }
        public double getOffsetX() { return offsetX; }
        public double getOffsetY() { return offsetY; }
        public double getOffsetZ() { return offsetZ; }
        public boolean isBackgroundEnabled() { return backgroundEnabled; }
        public double getBackgroundOpacity() { return backgroundOpacity; }
        public Color getBackgroundColor() {
            int rgb = Integer.parseInt(backgroundColor.replace("#", ""), 16);
            return Color.fromRGB(rgb);
        }
        public boolean isGlowing() { return glowing; }
        public Color getGlowColor() {
            int rgb = Integer.parseInt(glowColor.replace("#", ""), 16);
            return Color.fromRGB(rgb);
        }
        public List<String> getLines() { return lines; }
    }
}
