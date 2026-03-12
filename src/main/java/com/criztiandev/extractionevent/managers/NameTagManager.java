package com.criztiandev.extractionevent.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.criztiandev.extractionevent.ExtractionEventPlugin;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class NameTagManager {

    private static final String ANONYMOUS_NAME = "§8Anonymous";

    private final ExtractionEventPlugin plugin;
    private final Team hiddenTeam;

    // ConcurrentHashMap — packet listener reads these from the netty I/O thread
    private final Map<UUID, String> originalDisplayNames = new ConcurrentHashMap<>();
    private final Map<UUID, String> originalListNames    = new ConcurrentHashMap<>();


    private boolean revealMode = false;

    public NameTagManager(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        Team team = scoreboard.getTeam("LevHidden");
        if (team == null) {
            team = scoreboard.registerNewTeam("LevHidden");
        }
        
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        team.setOption(Team.Option.COLLISION_RULE,      Team.OptionStatus.NEVER);
        
        
        this.hiddenTeam = team;

        // ProtocolLib Packet Interception to actively destroy the nametag over the head
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, PacketType.Play.Server.ENTITY_METADATA) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
                            org.bukkit.entity.Entity entity = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).readSafely(0);
                            if (entity instanceof Player targetPlayer) {
                                if (originalDisplayNames.containsKey(targetPlayer.getUniqueId()) && !revealMode) {
                                    PacketContainer cloned = event.getPacket().deepClone();
                                    
                                    var watchableObjects = cloned.getDataValueCollectionModifier().readSafely(0);
                                    if (watchableObjects != null) {
                                        for (com.comphenix.protocol.wrappers.WrappedDataValue dataValue : watchableObjects) {
                                            if (dataValue.getIndex() == 3) {
                                                dataValue.setValue(false);
                                            }
                                        }
                                        cloned.getDataValueCollectionModifier().write(0, watchableObjects);
                                    }
                                    event.setPacket(cloned);
                                }
                            }
                        }
                    }
                }
            );
        }
    }

    // ── Hide / restore ───────────────────────────────────────────────────────

    /**
     * Applies anonymous identity to a warzone player.
     * If reveal mode is active, names stay real in tab/chat but nametag is still hidden.
     */
    public void hideNameTag(Player player) {
        UUID uuid = player.getUniqueId();

        if (!originalDisplayNames.containsKey(uuid)) {
            originalDisplayNames.put(uuid, player.getDisplayName());
            originalListNames.put(uuid, player.getPlayerListName());
        }

        if (!hiddenTeam.hasEntry(player.getName())) {
            hiddenTeam.addEntry(player.getName());
        }

        // In reveal mode names stay real (scoreboard team still hides the overhead tag)
        if (!revealMode) {
            // Note: DO NOT setDisplayName here, as modifying it raw breaks 1.19+ Chat Signatures 
            // causing the "Chat validation error". We handle chat anonymization via AsyncChatEvent.
            player.setPlayerListName(ANONYMOUS_NAME);
        }
    }

    /**
     * Checks if a player's true identity is currently concealed in a warzone.
     */
    public boolean isAnonymized(Player player) {
        return originalDisplayNames.containsKey(player.getUniqueId()) && !revealMode;
    }

    /**
     * Restores the player's original identity when leaving a warzone region.
     */
    public void restoreNameTag(Player player) {
        UUID uuid = player.getUniqueId();
        hiddenTeam.removeEntry(player.getName());

        String display = originalDisplayNames.remove(uuid);
        player.setDisplayName(display != null ? display : player.getName());

        String list = originalListNames.remove(uuid);
        player.setPlayerListName(list != null ? list : player.getName());
    }

    // ── Reveal mode (admin /lev showNameTags) ────────────────────────────────

    /**
     * Toggles server-wide reveal mode and immediately refreshes all online players
     * who are currently anonymized (still have an entry in originalDisplayNames).
     *
     * @return true if reveal mode is now ON, false if now OFF.
     */
    public boolean toggleRevealMode() {
        revealMode = !revealMode;

        // Refresh all currently-anonymized online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (!originalDisplayNames.containsKey(uuid)) continue; // not in warzone
     
        }
        return revealMode;
    }

    public boolean isRevealMode() {
        return revealMode;
    }

    /**
     * Returns the real (pre-anonymization) name of a player, or their current
     * display name if they were never anonymized. Used by KillEffectListener for
     * server-log purposes without leaking identity to players.
     */
    public String getRealName(UUID uuid) {
        String original = originalDisplayNames.get(uuid);
        if (original != null) return original;
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : uuid.toString();
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    public void cleanup() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            restoreNameTag(player);
        }
        if (hiddenTeam != null) {
            hiddenTeam.unregister();
        }
    }
}
