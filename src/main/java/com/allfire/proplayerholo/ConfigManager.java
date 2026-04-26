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
                ConfigurationSection p = profilesSection.getConfigurationSection(key);
                if (p == null) continue;

                // Text shadow
                boolean shadowEnabled = p.getBoolean("text-shadow.enabled", true);
                String shadowColor = p.getString("text-shadow.color", "#000000");
                int shadowOffsetX = p.getInt("text-shadow.offset-x", 1);
                int shadowOffsetY = p.getInt("text-shadow.offset-y", 1);

                ProfileConfig profile = new ProfileConfig(
                        key,
                        p.getString("permission"),
                        p.getString("name"),
                        p.getStringList("description"),
                        p.getString("mode", "TARGET_TRACKING"),
                        p.getInt("duration", 10),
                        p.getDouble("display.size", 1.0),
                        p.getDouble("display.scale-x", 0.0),
                        p.getDouble("display.scale-y", 0.0),
                        p.getDouble("display.scale-z", 0.0),
                        p.getString("display.position", "ABOVE"),
                        p.getString("display.billboard", "CENTER"),
                        p.getDouble("display.height-offset", 0.5),
                        p.getDouble("display.offset-x", 0.0),
                        p.getDouble("display.offset-y", 0.0),
                        p.getDouble("display.offset-z", 0.0),
                        p.getDouble("display.distance", 1.5),
                        shadowEnabled, shadowColor, shadowOffsetX, shadowOffsetY,
                        p.getBoolean("background.enabled", true),
                        p.getDouble("background.opacity", 0.3),
                        p.getString("background.color", "#000000"),
                        p.getBoolean("background.glowing", false),
                        p.getString("background.glow-color", "#FFFFFF"),
                        p.getInt("background.glow-intensity", 0),
                        p.getStringList("lines")
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

    // ─── Enums ────────────────────────────────────────

    public enum HologramMode {
        TARGET_TRACKING,
        VIEWER_FACE
    }

    public enum HologramPosition {
        ABOVE, FRONT, BACK, LEFT, RIGHT, BELOW, ABOVE_FRONT, CUSTOM
    }

    public enum BillboardMode {
        FIXED, VERTICAL, HORIZONTAL, CENTER
    }

    // ─── Profile Config ──────────────────────────────

    public static class ProfileConfig {
        private final String id, permission, name;
        private final List<String> description;
        private final HologramMode mode;
        private final int duration;
        private final double size, scaleX, scaleY, scaleZ;
        private final HologramPosition position;
        private final BillboardMode billboard;
        private final double heightOffset, offsetX, offsetY, offsetZ, distance;
        private final boolean shadowEnabled;
        private final String shadowColor;
        private final int shadowOffsetX, shadowOffsetY;
        private final boolean backgroundEnabled;
        private final double backgroundOpacity;
        private final String backgroundColor;
        private final boolean glowing;
        private final String glowColor;
        private final int glowIntensity;
        private final List<String> lines;

        public ProfileConfig(String id, String permission, String name, List<String> description,
                             String mode, int duration, double size, double scaleX, double scaleY, double scaleZ,
                             String position, String billboard, double heightOffset,
                             double offsetX, double offsetY, double offsetZ, double distance,
                             boolean shadowEnabled, String shadowColor, int shadowOffsetX, int shadowOffsetY,
                             boolean backgroundEnabled, double backgroundOpacity, String backgroundColor,
                             boolean glowing, String glowColor, int glowIntensity, List<String> lines) {
            this.id = id;
            this.permission = permission;
            this.name = name;
            this.description = description;
            this.mode = parseMode(mode);
            this.duration = duration;
            this.size = size;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.position = parsePosition(position);
            this.billboard = parseBillboard(billboard);
            this.heightOffset = heightOffset;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.distance = distance;
            this.shadowEnabled = shadowEnabled;
            this.shadowColor = shadowColor;
            this.shadowOffsetX = shadowOffsetX;
            this.shadowOffsetY = shadowOffsetY;
            this.backgroundEnabled = backgroundEnabled;
            this.backgroundOpacity = backgroundOpacity;
            this.backgroundColor = backgroundColor;
            this.glowing = glowing;
            this.glowColor = glowColor;
            this.glowIntensity = glowIntensity;
            this.lines = lines;
        }

        private HologramMode parseMode(String m) {
            try { return HologramMode.valueOf(m.toUpperCase()); }
            catch (IllegalArgumentException e) { return HologramMode.TARGET_TRACKING; }
        }

        private HologramPosition parsePosition(String p) {
            try { return HologramPosition.valueOf(p.toUpperCase()); }
            catch (IllegalArgumentException e) { return HologramPosition.ABOVE; }
        }

        private BillboardMode parseBillboard(String b) {
            try { return BillboardMode.valueOf(b.toUpperCase()); }
            catch (IllegalArgumentException e) { return BillboardMode.CENTER; }
        }

        // Getters
        public String getId() { return id; }
        public String getPermission() { return permission; }
        public String getName() { return name; }
        public List<String> getDescription() { return description; }
        public HologramMode getMode() { return mode; }
        public int getDuration() { return duration; }
        public double getSize() { return size; }
        public double getScaleX() { return scaleX; }
        public double getScaleY() { return scaleY; }
        public double getScaleZ() { return scaleZ; }
        public HologramPosition getPosition() { return position; }
        public BillboardMode getBillboard() { return billboard; }
        public double getHeightOffset() { return heightOffset; }
        public double getOffsetX() { return offsetX; }
        public double getOffsetY() { return offsetY; }
        public double getOffsetZ() { return offsetZ; }
        public double getDistance() { return distance; }
        public boolean isShadowEnabled() { return shadowEnabled; }
        public String getShadowColor() { return shadowColor; }
        public int getShadowOffsetX() { return shadowOffsetX; }
        public int getShadowOffsetY() { return shadowOffsetY; }
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
        public int getGlowIntensity() { return glowIntensity; }
        public List<String> getLines() { return lines; }
    }
}
