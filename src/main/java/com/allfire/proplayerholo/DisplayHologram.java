package com.allfire.proplayerholo;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DisplayHologram {
    private final Player target;
    private final Player viewer;
    private final JavaPlugin plugin;
    private final ConfigManager.ProfileConfig profileConfig;
    private TextDisplay display;

    public DisplayHologram(Player target, Player viewer, JavaPlugin plugin, ConfigManager.ProfileConfig profileConfig) {
        this.target = target;
        this.viewer = viewer;
        this.plugin = plugin;
        this.profileConfig = profileConfig;
    }

    public void create() {
        try {
            Location loc = calculatePosition();
            if (target.getWorld() == null) return;

            display = target.getWorld().spawn(loc, TextDisplay.class);
            if (display == null) return;

            display.addScoreboardTag("pph_hologram");

            // Billboard
            switch (profileConfig.getBillboard()) {
                case FIXED -> display.setBillboard(Display.Billboard.FIXED);
                case VERTICAL -> display.setBillboard(Display.Billboard.VERTICAL);
                case HORIZONTAL -> display.setBillboard(Display.Billboard.HORIZONTAL);
                default -> display.setBillboard(Display.Billboard.CENTER);
            }

            display.setSeeThrough(false);

            // Background
            if (profileConfig.isBackgroundEnabled()) {
                display.setBackgroundColor(org.bukkit.Color.fromARGB(
                        (int) (profileConfig.getBackgroundOpacity() * 255),
                        profileConfig.getBackgroundColor().getRed(),
                        profileConfig.getBackgroundColor().getGreen(),
                        profileConfig.getBackgroundColor().getBlue()
                ));
            }

            // Glow
            if (profileConfig.isGlowing()) {
                display.setGlowing(true);
                display.setGlowColorOverride(profileConfig.getGlowColor());
            }

            // Scale
            float sx = profileConfig.getScaleX() > 0 ? (float) profileConfig.getScaleX() : (float) profileConfig.getSize();
            float sy = profileConfig.getScaleY() > 0 ? (float) profileConfig.getScaleY() : (float) profileConfig.getSize();
            float sz = profileConfig.getScaleZ() > 0 ? (float) profileConfig.getScaleZ() : (float) profileConfig.getSize();

            display.setTransformation(new Transformation(
                    new Vector3f(), new Quaternionf(),
                    new Vector3f(sx, sy, sz), new Quaternionf()
            ));

            // Visibility
            display.setVisibleByDefault(false);
            viewer.showEntity(plugin, display);

            updateText();
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating hologram: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Location calculatePosition() {
        Location baseLoc;
        boolean isViewerMode = profileConfig.getMode() == ConfigManager.HologramMode.VIEWER_FACE;
        Location origin = isViewerMode ? viewer.getLocation().clone() : target.getLocation().clone();

        if (profileConfig.getPosition() == ConfigManager.HologramPosition.CUSTOM) {
            return origin.clone().add(profileConfig.getOffsetX(), profileConfig.getOffsetY(), profileConfig.getOffsetZ());
        }

        Vector direction;
        if (isViewerMode) {
            direction = origin.getDirection().setY(0).normalize();
        } else {
            direction = viewer.getLocation().toVector().subtract(origin.toVector()).setY(0).normalize();
        }
        if (direction.length() < 0.1) direction = origin.getDirection().setY(0).normalize();

        Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        double dist = profileConfig.getDistance();

        switch (profileConfig.getPosition()) {
            case FRONT -> baseLoc = origin.clone().add(direction.clone().multiply(dist));
            case BACK -> baseLoc = origin.clone().add(direction.clone().multiply(-dist));
            case LEFT -> baseLoc = origin.clone().add(right.clone().multiply(-dist));
            case RIGHT -> baseLoc = origin.clone().add(right.clone().multiply(dist));
            case BELOW -> baseLoc = origin.clone().add(0, -dist, 0);
            case ABOVE_FRONT -> baseLoc = origin.clone()
                    .add(direction.clone().multiply(dist * 0.7))
                    .add(0, profileConfig.getHeightOffset() + dist, 0);
            default -> baseLoc = origin.clone().add(0, profileConfig.getHeightOffset() + dist, 0);
        }

        return baseLoc.add(profileConfig.getOffsetX(), profileConfig.getOffsetY(), profileConfig.getOffsetZ());
    }

    public void updateText() {
        if (display == null || !display.isValid()) return;
        try {
            StringBuilder text = new StringBuilder();
            for (String line : profileConfig.getLines()) {
                String processed = PlaceholderAPI.setPlaceholders(target, line);
                if (profileConfig.isShadowEnabled()) {
                    processed = "<shadow:" + profileConfig.getShadowColor() + ":"
                            + profileConfig.getShadowOffsetX() + ":"
                            + profileConfig.getShadowOffsetY() + ">"
                            + processed + "</shadow>";
                }
                text.append(processed).append("\n");
            }
            String finalText = text.toString().trim();
            if (!finalText.isEmpty()) {
                display.text(MiniMessage.miniMessage().deserialize(finalText));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error updating hologram text: " + e.getMessage());
        }
    }

    public void remove() {
        if (display != null && display.isValid()) display.remove();
    }

    public void updatePosition() {
        if (display != null && display.isValid() && target.isOnline() && viewer.isOnline()) {
            try {
                display.teleport(calculatePosition());
            } catch (Exception e) {
                plugin.getLogger().warning("Error updating hologram position: " + e.getMessage());
            }
        }
    }
}
