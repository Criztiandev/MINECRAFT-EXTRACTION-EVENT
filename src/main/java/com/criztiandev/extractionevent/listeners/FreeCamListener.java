package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class FreeCamListener implements Listener {

    private static final String BYPASS_PERMISSION = "lev.bypass";

    private final ExtractionEventPlugin plugin;
    private final double maxReachSq;

    public FreeCamListener(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        double reach = plugin.getConfig().getDouble("warzone.max-reach", 5.5);
        this.maxReachSq = reach * reach;
    }

    private boolean isInWarzone(Player player) {
        return plugin.getRegionPresenceTask().isInAnyRegion(player.getUniqueId());
    }

    // ── 1. Block Spectator mode ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() != GameMode.SPECTATOR) return;
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) return;
        if (!isInWarzone(player)) return;

        event.setCancelled(true);
        player.sendMessage("§cSpectator mode is not allowed inside the warzone!");
    }

    // ── 2. Block non-creative flight ─────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFlightToggle(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        if (!event.isFlying()) return;
        if (player.hasPermission(BYPASS_PERMISSION)) return;
        if (!isInWarzone(player)) return;

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.sendMessage("§cFlight is not allowed inside the warzone!");
    }

    // ── 3. Extended-reach detection (FreeCam interaction abuse) ─────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) return;
        if (!isInWarzone(player)) return;

        double distSq = event.getClickedBlock().getLocation()
                .add(0.5, 0.5, 0.5)
                .distanceSquared(player.getEyeLocation());
        if (distSq > maxReachSq) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot interact with objects that far away!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) return;
        if (!isInWarzone(player)) return;

        double distSq = event.getRightClicked().getLocation()
                .distanceSquared(player.getEyeLocation());
        if (distSq > maxReachSq) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot interact with entities that far away!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) return;
        if (!isInWarzone(player)) return;

        double distSq = event.getRightClicked().getLocation()
                .distanceSquared(player.getEyeLocation());
        if (distSq > maxReachSq) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) return;
        if (!isInWarzone(player)) return;

        double distSq = event.getBlock().getLocation()
                .add(0.5, 0.5, 0.5)
                .distanceSquared(player.getEyeLocation());
        if (distSq > maxReachSq) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot break blocks that far away!");
        }
    }

    // ── Package-visible for tests ─────────────────────────────────────────────

    double getMaxReachSq() {
        return maxReachSq;
    }
}
