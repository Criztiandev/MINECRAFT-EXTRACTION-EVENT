package com.criztiandev.extractionevent.managers;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player identity concealment inside warzone regions.
 *
 * Strategy:
 *   1. Scoreboard team "LevHidden" — NAME_TAG_VISIBILITY = NEVER (hides head nametag).
 *   2. COLLISION_RULE = NEVER (disables entity collision used for radar detection).
 *   3. player.setPlayerListName("§8Anonymous") — masks identity in tab list.
 *   4. player.setDisplayName("§8Anonymous") — masks identity in chat.
 *
 * Admin reveal mode (/lev showNameTags):
 *   A server-wide admin toggle. When enabled, all active warzone players have their
 *   real names temporarily restored so admins can identify who's who. The scoreboard
 *   team still suppresses nametags visually above heads, so observers on the ground
 *   cannot benefit from it — real names are visible only in tab list and chat kill
 *   messages while in reveal mode.
 */
public class NameTagManager {

    private static final String ANONYMOUS_NAME = "§8Anonymous";

    private final ExtractionEventPlugin plugin;
    private final Team hiddenTeam;

    /** UUID → original display name (before anonymization). */
    private final Map<UUID, String> originalDisplayNames = new HashMap<>();
    /** UUID → original tab-list name. */
    private final Map<UUID, String> originalListNames    = new HashMap<>();

    /**
     * When true: anonymized players have their original tab-list / display names
     * temporarily restored. The overhead nametag is still hidden by the scoreboard
     * team so it cannot be seen from in-game without the tab list.
     */
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
            player.setDisplayName(ANONYMOUS_NAME);
            player.setPlayerListName(ANONYMOUS_NAME);
        }
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

            if (revealMode) {
                // Restore real names (overhead tag still hidden by team)
                player.setDisplayName(originalDisplayNames.get(uuid));
                player.setPlayerListName(originalListNames.get(uuid) != null
                        ? originalListNames.get(uuid) : player.getName());
            } else {
                // Re-apply anonymous
                player.setDisplayName(ANONYMOUS_NAME);
                player.setPlayerListName(ANONYMOUS_NAME);
            }
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
