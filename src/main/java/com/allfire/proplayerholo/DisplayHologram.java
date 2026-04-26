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
            
            if (target.getWorld() == null) {
                plugin.getLogger().warning("Target world is null for player: " + target.getName());
                return;
            }
            
            display = target.getWorld().spawn(loc, TextDisplay.class);
            
            if (display == null) {
                plugin.getLogger().warning("Failed to spawn TextDisplay for target: " + target.getName());
                return;
            }

            display.addScoreboardTag("pph_hologram");
            
            // Применяем billboard
            switch (profileConfig.getBillboard()) {
                case FIXED:
                    display.setBillboard(Display.Billboard.FIXED);
                    break;
                case VERTICAL:
                    display.setBillboard(Display.Billboard.VERTICAL);
                    break;
                case HORIZONTAL:
                    display.setBillboard(Display.Billboard.HORIZONTAL);
                    break;
                case CENTER:
                default:
                    display.setBillboard(Display.Billboard.CENTER);
                    break;
            }
            
            display.setSeeThrough(false);

            if (profileConfig.isBackgroundEnabled()) {
                display.setBackgroundColor(org.bukkit.Color.fromARGB(
                        (int)(profileConfig.getBackgroundOpacity() * 255),
                        profileConfig.getBackgroundColor().getRed(),
                        profileConfig.getBackgroundColor().getGreen(),
                        profileConfig.getBackgroundColor().getBlue()
                ));
            }

            if (profileConfig.isGlowing()) {
                display.setGlowing(true);
                display.setGlowColorOverride(profileConfig.getGlowColor());
            }

            // Настройка размера: если указаны scale-x/y/z, используем их, иначе общий size
            float sx = profileConfig.getScaleX() > 0 ? (float) profileConfig.getScaleX() : (float) profileConfig.getSize();
            float sy = profileConfig.getScaleY() > 0 ? (float) profileConfig.getScaleY() : (float) profileConfig.getSize();
            float sz = profileConfig.getScaleZ() > 0 ? (float) profileConfig.getScaleZ() : (float) profileConfig.getSize();
            
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(sx, sy, sz),
                    new Quaternionf()
            ));

            // Скрываем от всех, показываем только viewer
            display.setVisibleByDefault(false);
            viewer.showEntity(plugin, display);

            updateText();
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating hologram: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Location calculatePosition() {
        Location targetLoc = target.getLocation().clone();
        
        // Для CUSTOM позиции — просто применяем offset относительно цели
        if (profileConfig.getPosition() == ConfigManager.HologramPosition.CUSTOM) {
            return targetLoc.clone()
                    .add(profileConfig.getOffsetX(), profileConfig.getOffsetY(), profileConfig.getOffsetZ());
        }
        
        // Направление от цели к viewer
        Vector directionToViewer = viewer.getLocation().toVector()
                .subtract(targetLoc.toVector())
                .setY(0)
                .normalize();
        
        if (directionToViewer.length() < 0.1) {
            directionToViewer = targetLoc.getDirection().setY(0).normalize();
        }
        
        Vector right = new Vector(-directionToViewer.getZ(), 0, directionToViewer.getX()).normalize();
        
        Location baseLoc;
        double dist = profileConfig.getDistance();
        
        switch (profileConfig.getPosition()) {
            case FRONT:
                baseLoc = targetLoc.clone().add(directionToViewer.clone().multiply(dist));
                break;
            case BACK:
                baseLoc = targetLoc.clone().add(directionToViewer.clone().multiply(-dist));
                break;
            case LEFT:
                baseLoc = targetLoc.clone().add(right.clone().multiply(-dist));
                break;
            case RIGHT:
                baseLoc = targetLoc.clone().add(right.clone().multiply(dist));
                break;
            case BELOW:
                baseLoc = targetLoc.clone().add(0, -dist, 0);
                break;
            case ABOVE_FRONT:
                baseLoc = targetLoc.clone()
                        .add(directionToViewer.clone().multiply(dist * 0.7))
                        .add(0, profileConfig.getHeightOffset() + dist, 0);
                break;
            case ABOVE:
            default:
                baseLoc = targetLoc.clone().add(0, profileConfig.getHeightOffset() + dist, 0);
                break;
        }
        
        return baseLoc.add(profileConfig.getOffsetX(), profileConfig.getOffsetY(), profileConfig.getOffsetZ());
    }

    public void updateText() {
        if (display == null || !display.isValid()) return;

        try {
            StringBuilder text = new StringBuilder();
            for (String line : profileConfig.getLines()) {
                String processed = PlaceholderAPI.setPlaceholders(target, line);
                text.append(processed).append("\n");
            }

            String finalText = text.toString().trim();
            if (!finalText.isEmpty()) {
                Component component = MiniMessage.miniMessage().deserialize(finalText);
                display.text(component);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error updating hologram text: " + e.getMessage());
        }
    }

    public void remove() {
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    public void updatePosition() {
        if (display != null && display.isValid() && target.isOnline() && viewer.isOnline()) {
            try {
                Location loc = calculatePosition();
                display.teleport(loc);
            } catch (Exception e) {
                plugin.getLogger().warning("Error updating hologram position: " + e.getMessage());
            }
        }
    }
}
