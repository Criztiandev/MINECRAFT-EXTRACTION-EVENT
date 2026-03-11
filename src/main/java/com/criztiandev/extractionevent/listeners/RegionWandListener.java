package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import com.criztiandev.extractionevent.models.RegionSelection;
import com.criztiandev.extractionevent.utils.WandUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the region selector wand.
 *
 * On Pos1 (left-click): shows a pulsing RED_WOOL fake block at the clicked position.
 * On Pos2 (right-click): shows a LIME_WOOL fake block, then draws the full square
 *   outline of the 2D selection footprint using RED_WOOL — only visible to the admin,
 *   world is NOT modified (sendBlockChange). Reverts after 10 seconds.
 */
public class RegionWandListener implements Listener {

    private static final int REVERT_TICKS = 200; // 10 seconds
    /** Max outline blocks sent to avoid lag on very large regions (skip every N). */
    private static final int MAX_OUTLINE_BLOCKS = 200;

    private final ExtractionEventPlugin plugin;
    /** Pending revert task IDs per player, so we can cancel on new selection. */
    private final Map<UUID, Integer> pendingReverts = new HashMap<>();

    public RegionWandListener(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !WandUtil.isWand(plugin, item)) return;
        if (!player.hasPermission("extractionevent.admin")) return;

        Action action = event.getAction();
        Block block = event.getClickedBlock();
        if (block == null) return;

        event.setCancelled(true);
        RegionSelection sel = plugin.getRegionManager().getOrCreateSelection(player.getUniqueId());

        // Cancel any existing revert task before showing new selection
        cancelPendingRevert(player.getUniqueId());

        if (action == Action.LEFT_CLICK_BLOCK) {
            sel.setPos1(block.getLocation());
            sendActionBar(player, "§c✦ Pos1 §fset to " + formatPos(block));
            player.sendMessage("§d[Wand] §cPos1 §7→ " + formatPos(block));
            showSingleMarker(player, block.getLocation(), Material.RED_WOOL);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            sel.setPos2(block.getLocation());
            sendActionBar(player, "§a✦ Pos2 §fset to " + formatPos(block));
            player.sendMessage("§d[Wand] §aPos2 §7→ " + formatPos(block));
            showSingleMarker(player, block.getLocation(), Material.LIME_WOOL);
        }

        if (sel.isComplete()) {
            int dx = sel.getMaxX() - sel.getMinX() + 1;
            int dy = sel.getMaxY() - sel.getMinY() + 1;
            int dz = sel.getMaxZ() - sel.getMinZ() + 1;
            long volume = (long) dx * dy * dz;

            player.sendMessage("§d[Wand] §7Selection: §e" + dx + "§7×§e" + dy + "§7×§e" + dz
                    + " §7= §e" + volume + " §7blocks");
            player.sendMessage("§d[Wand] §7Run §e/lrev create <name> §7to save.");

            // Draw the fake square outline and schedule its removal
            List<Location> outline = buildOutline(player.getWorld(), sel);
            showOutline(player, outline, Material.RED_WOOL);
            scheduleRevert(player, outline);

            // Overlap warning
            for (LevRegion existing : plugin.getRegionManager().getRegions()) {
                if (existing.getWorld().equals(sel.getWorldName()) && overlaps(sel, existing)) {
                    player.sendMessage("§c[Wand] §eWarning: §7overlaps with region §e" + existing.getId() + "§7!");
                }
            }
        }
    }

    // ── Fake-block helpers ────────────────────────────────────────────────────

    /** Shows a single fake block at the clicked position to mark a corner. */
    private static void showSingleMarker(Player player, Location loc, Material mat) {
        // Show at the block surface (top face)
        Location top = loc.clone().add(0, 1, 0);
        player.sendBlockChange(top, Bukkit.createBlockData(mat));
    }

    /**
     * Builds the 4 perimeter edges of the selection footprint at the floor Y level
     * (minY). Uses sendBlockChange so ONLY the admin player sees these — the world
     * is not modified at all.
     */
    private static List<Location> buildOutline(World world, RegionSelection sel) {
        List<Location> positions = new ArrayList<>();
        if (world == null) return positions;

        int y  = sel.getMinY();
        int x1 = sel.getMinX(), x2 = sel.getMaxX();
        int z1 = sel.getMinZ(), z2 = sel.getMaxZ();

        int totalPerimeter = 2 * (x2 - x1 + 1) + 2 * (z2 - z1 + 1) - 4;
        int step = Math.max(1, totalPerimeter / MAX_OUTLINE_BLOCKS);

        // South edge (z1) and North edge (z2) — vary X
        for (int x = x1; x <= x2; x += step) {
            positions.add(new Location(world, x, y, z1));
            positions.add(new Location(world, x, y, z2));
        }
        // West edge (x1) and East edge (x2) — vary Z
        for (int z = z1; z <= z2; z += step) {
            positions.add(new Location(world, x1, y, z));
            positions.add(new Location(world, x2, y, z));
        }
        return positions;
    }

    private static void showOutline(Player player, List<Location> positions, Material mat) {
        var blockData = Bukkit.createBlockData(mat);
        for (Location loc : positions) {
            player.sendBlockChange(loc, blockData);
        }
    }

    /** Reverts all outline blocks back to actual world blocks after REVERT_TICKS. */
    private void scheduleRevert(Player player, List<Location> positions) {
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingReverts.remove(player.getUniqueId());
            if (!player.isOnline()) return;
            for (Location loc : positions) {
                if (loc.getWorld() != null) {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }
        }, REVERT_TICKS).getTaskId();
        pendingReverts.put(player.getUniqueId(), taskId);
    }

    private void cancelPendingRevert(UUID uuid) {
        Integer taskId = pendingReverts.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────

    private static String formatPos(Block b) {
        return "(" + b.getX() + ", " + b.getY() + ", " + b.getZ() + ")";
    }

    @SuppressWarnings("deprecation")
    private static void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(message));
    }

    private static boolean overlaps(RegionSelection sel, LevRegion r) {
        return sel.getMaxX() >= r.getMinX() && sel.getMinX() <= r.getMaxX()
            && sel.getMaxZ() >= r.getMinZ() && sel.getMinZ() <= r.getMaxZ()
            && sel.getMaxY() >= r.getMinY() && sel.getMinY() <= r.getMaxY();
    }
}
