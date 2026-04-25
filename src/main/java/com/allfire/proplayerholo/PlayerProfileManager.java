package com.allfire.proplayerholo;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerProfileManager {
    private final Map<UUID, String> playerProfiles = new HashMap<>();
    private final ConfigManager configManager;

    public PlayerProfileManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void setProfile(Player player, String profileId) {
        playerProfiles.put(player.getUniqueId(), profileId);
    }

    public String getProfile(Player player) {
        return playerProfiles.getOrDefault(player.getUniqueId(), configManager.getDefaultProfile());
    }

    public ConfigManager.ProfileConfig getProfileConfig(Player player) {
        String profileId = getProfile(player);
        ConfigManager.ProfileConfig profile = configManager.getProfile(profileId);

        if (profile == null) {
            profile = configManager.getProfile(configManager.getDefaultProfile());
        }

        if (profile != null && !player.hasPermission(profile.getPermission())) {
            for (ConfigManager.ProfileConfig p : configManager.getAllProfiles()) {
                if (player.hasPermission(p.getPermission())) {
                    return p;
                }
            }
            return configManager.getProfile(configManager.getDefaultProfile());
        }

        return profile;
    }

    public void removePlayer(Player player) {
        playerProfiles.remove(player.getUniqueId());
    }
}
