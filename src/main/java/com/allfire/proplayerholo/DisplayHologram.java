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
        Location loc = target.getLocation().add(0, profileConfig.getHeightOffset(), 0);
        display = target.getWorld().spawn(loc, TextDisplay.class);

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

        display.setVisibleByDefault(false);
        viewer.showEntity(plugin, display);

        updateText();
    }

    public void updateText() {
        if (display == null || !display.isValid()) return;

        StringBuilder text = new StringBuilder();
        for (String line : profileConfig.getLines()) {
            String processed = PlaceholderAPI.setPlaceholders(target, line);
            text.append(processed).append("\n");
        }

        Component component = MiniMessage.miniMessage().deserialize(text.toString().trim());
        display.text(component);
    }

    public void remove() {
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    public void updatePosition() {
        if (display != null && display.isValid() && target.isOnline()) {
            Location loc = target.getLocation().add(0, profileConfig.getHeightOffset(), 0);
            display.teleport(loc);
        }
    }
}
