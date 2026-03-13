package com.criztiandev.extractionevent.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MinimapHideManager {

    private static final int ENTITY_FLAG_INDEX  = 0;
    private static final byte INVISIBLE_BIT     = (byte) 0x20; // bit 5
    // Particle proximity threshold squared (16 blocks) — beyond this we don't bother
    private static final double PARTICLE_RANGE_SQ = 16.0 * 16.0;

    private final ExtractionEventPlugin plugin;
    private final ProtocolManager protocolManager;
    private final boolean debugEnabled;

    private final Set<UUID> warzoneSet = java.util.concurrent.ConcurrentHashMap.newKeySet(128);

    public MinimapHideManager(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.debugEnabled = plugin.getConfig().getBoolean("settings.debug", false);
        registerPacketFirewall();
    }


    public void setWarzoneSet(Set<UUID> current) {
        warzoneSet.clear();
        warzoneSet.addAll(current);
    }


    public void onPlayerEnterRegion(Player entering, LevRegion region) {
        UUID enteringId = entering.getUniqueId();
        warzoneSet.add(enteringId);

        for (Player member : plugin.getRegionPresenceTask().getCachedPlayersInRegion(region)) {
            if (member.getUniqueId().equals(enteringId)) continue;
            entering.hidePlayer(plugin, member);
            entering.showPlayer(plugin, member);
        }

        for (Player outsider : Bukkit.getOnlinePlayers()) {
            if (warzoneSet.contains(outsider.getUniqueId())) continue;
            outsider.hidePlayer(plugin, entering);
        }
    }

    public void onPlayerLeaveRegion(Player leaving, LevRegion region) {
        warzoneSet.remove(leaving.getUniqueId());

        for (Player outsider : Bukkit.getOnlinePlayers()) {
            if (warzoneSet.contains(outsider.getUniqueId())) continue;
            outsider.showPlayer(plugin, leaving);
        }
    }

    private void registerPacketFirewall() {
        protocolManager.addPacketListener(new PacketAdapter(plugin,
                PacketType.Play.Server.SPAWN_ENTITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (warzoneSet.isEmpty()) return;

                Player recipient = event.getPlayer();
                if (warzoneSet.contains(recipient.getUniqueId())) return;

                try {
                    Entity entity = event.getPacket()
                            .getEntityModifier(recipient.getWorld())
                            .readSafely(0);

                    if (entity instanceof Player spawned
                            && warzoneSet.contains(spawned.getUniqueId())) {
                        event.setCancelled(true);
                        logDebug("Blocked SPAWN_ENTITY for " + spawned.getName()
                                + " to outsider " + recipient.getName());
                    }
                } catch (Exception ignored) {}
            }
        });

        // ── Layer 2: Strip invisible flag from ENTITY_METADATA ────────────────
        // Warzone players can physically see each other, but the minimap radar
        // reads this same metadata to infer player status. By clearing bit 5
        // (the INVISIBLE flag) on entity-flags index 0, we ensure invisible
        // players are indistinguishable from normal ones on the radar.
        protocolManager.addPacketListener(new PacketAdapter(plugin,
                PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (warzoneSet.isEmpty()) return;

                Player recipient = event.getPlayer();
                if (!warzoneSet.contains(recipient.getUniqueId())) return;

                try {
                    Entity entity = event.getPacket()
                            .getEntityModifier(recipient.getWorld())
                            .readSafely(0);

                    if (!(entity instanceof Player target)) return;
                    if (!warzoneSet.contains(target.getUniqueId())) return;
                    if (!target.hasPotionEffect(PotionEffectType.INVISIBILITY)) return;

                    List<WrappedDataValue> values = event.getPacket()
                            .getDataValueCollectionModifier()
                            .readSafely(0);
                    if (values == null) return;

                    boolean needsPatch = values.stream()
                            .anyMatch(v -> v.getIndex() == ENTITY_FLAG_INDEX
                                    && v.getValue() instanceof Byte b
                                    && (b & INVISIBLE_BIT) != 0);
                    if (!needsPatch) return;

                    PacketContainer cloned = event.getPacket().deepClone();
                    List<WrappedDataValue> clonedValues = cloned
                            .getDataValueCollectionModifier()
                            .readSafely(0);
                    if (clonedValues != null) {
                        for (WrappedDataValue v : clonedValues) {
                            if (v.getIndex() == ENTITY_FLAG_INDEX && v.getValue() instanceof Byte b) {
                                v.setValue((byte) (b & ~INVISIBLE_BIT));
                            }
                        }
                        cloned.getDataValueCollectionModifier().write(0, clonedValues);
                    }
                    event.setPacket(cloned);
                    logDebug("Stripped invisible flag from metadata of " + target.getName()
                            + " for " + recipient.getName());
                } catch (Exception ignored) {}
            }
        });

        // ── Layer 3: Cancel particle packets near invisible warzone players ────
        // Counters the texture-pack exploit where players replace particle
        // textures with visible markers to track invisible enemies.
        // We cancel WORLD_PARTICLES packets sent to warzone recipients when the
        // particle origin is within range of any currently-invisible warzone player.
        protocolManager.addPacketListener(new PacketAdapter(plugin,
                PacketType.Play.Server.WORLD_PARTICLES) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (warzoneSet.isEmpty()) return;

                Player recipient = event.getPlayer();
                if (!warzoneSet.contains(recipient.getUniqueId())) return;

                try {
                    double px = event.getPacket().getDoubles().readSafely(0);
                    double py = event.getPacket().getDoubles().readSafely(1);
                    double pz = event.getPacket().getDoubles().readSafely(2);

                    org.bukkit.World world = recipient.getWorld();

                    for (UUID uuid : warzoneSet) {
                        if (uuid.equals(recipient.getUniqueId())) continue;
                        Player target = Bukkit.getPlayer(uuid);
                        if (target == null || !target.isOnline()) continue;
                        if (!target.hasPotionEffect(PotionEffectType.INVISIBILITY)) continue;
                        if (!target.getWorld().equals(world)) continue;

                        Location tLoc = target.getLocation();
                        double dx = px - tLoc.getX();
                        double dy = py - tLoc.getY();
                        double dz = pz - tLoc.getZ();
                        if (dx * dx + dy * dy + dz * dz <= PARTICLE_RANGE_SQ) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void logDebug(String msg) {
        if (debugEnabled) {
            plugin.getLogger().warning("[MinimapHide] " + msg);
        }
    }
}
