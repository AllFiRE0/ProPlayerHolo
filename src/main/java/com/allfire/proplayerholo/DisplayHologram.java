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
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DisplayHologram {
    private final Player target;
    private final JavaPlugin plugin;
    private final ConfigManager.ProfileConfig profileConfig;
    private TextDisplay display;

    public DisplayHologram(Player target, JavaPlugin plugin, ConfigManager.ProfileConfig profileConfig) {
        this.target = target;
        this.plugin = plugin;
        this.profileConfig = profileConfig;
    }

    public void create(Player viewer) {
        try {
            Location loc = target.getLocation().add(0, profileConfig.getHeightOffset(), 0);
            
            // Проверяем, что мир существует и загружен
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
            display.setBillboard(Display.Billboard.VERTICAL);
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

            float scale = (float) profileConfig.getSize();
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()
            ));

            // Скрываем от всех, показываем только viewer
            display.setVisibleByDefault(false);
            viewer.showEntity(plugin, display);

            updateText();
            
            plugin.getLogger().info("Hologram created for target: " + target.getName() + 
                                   ", viewer: " + viewer.getName() + 
                                   ", location: " + loc.toString());
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating hologram: " + e.getMessage());
            e.printStackTrace();
        }
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
        if (display != null && display.isValid() && target.isOnline()) {
            try {
                Location loc = target.getLocation().add(0, profileConfig.getHeightOffset(), 0);
                display.teleport(loc);
            } catch (Exception e) {
                plugin.getLogger().warning("Error updating hologram position: " + e.getMessage());
            }
        }
    }
}
