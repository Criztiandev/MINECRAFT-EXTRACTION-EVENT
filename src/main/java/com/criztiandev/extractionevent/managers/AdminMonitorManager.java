package com.criztiandev.extractionevent.managers;

import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which admin-only monitor features each admin player has toggled on.
 * Per-admin, per-feature — admins independently control their own view.
 */
public class AdminMonitorManager {

    public enum Feature {
        NAMETAGS, CHAT, MAP;

        public static Feature fromString(String s) {
            return switch (s.toLowerCase()) {
                case "nametags" -> NAMETAGS;
                case "chat"     -> CHAT;
                case "map"      -> MAP;
                default         -> null;
            };
        }
    }

    private final Map<UUID, Set<Feature>> enabled = new ConcurrentHashMap<>();

    public boolean toggle(UUID adminId, Feature feature) {
        Set<Feature> set = enabled.computeIfAbsent(adminId, k -> EnumSet.noneOf(Feature.class));
        if (set.contains(feature)) {
            set.remove(feature);
            return false;
        } else {
            set.add(feature);
            return true;
        }
    }

    public boolean has(Player admin, Feature feature) {
        Set<Feature> set = enabled.get(admin.getUniqueId());
        return set != null && set.contains(feature);
    }

    public void cleanup(UUID adminId) {
        enabled.remove(adminId);
    }
}
