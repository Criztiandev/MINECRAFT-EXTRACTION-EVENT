package com.criztiandev.extractionevent.managers;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NameTagManager {

    private final ExtractionEventPlugin plugin;
    private final Team hiddenTeam;

    public NameTagManager(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        Team team = scoreboard.getTeam("LevHidden");
        if (team == null) {
            team = scoreboard.registerNewTeam("LevHidden");
        }
        
        // This is the core logic that prevents the name tag above the head from showing
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        this.hiddenTeam = team;
    }

    public void hideNameTag(Player player) {
        if (!hiddenTeam.hasEntry(player.getName())) {
            hiddenTeam.addEntry(player.getName());
        }
    }

    public void restoreNameTag(Player player) {
        if (hiddenTeam.hasEntry(player.getName())) {
            hiddenTeam.removeEntry(player.getName());
        }
    }
    
    public void cleanup() {
        if (hiddenTeam != null) {
            hiddenTeam.unregister();
        }
    }
}
