package com.criztiandev.extractionevent.commands;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import com.criztiandev.extractionevent.utils.WandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Main command: /lev (alias: /ee, /extractionevent)
 *
 * Sub-commands:
 *   /lev                       → opens the region admin GUI
 *   /lev gui                   → same as above
 *   /lev wand                  → gives region-selection wand
 *   /lev create <name>         → creates region from current selection
 *   /lev remove <name>         → removes region
 *   /lev list                  → lists all regions
 *   /lev showNameTags          → toggles server-wide real-name reveal mode
 *   /lev toggle <region> <feature> [on|off]  → per-region feature flag control
 *
 * All sub-commands require extractionevent.admin.
 * Tab-completion is fully implemented for all args.
 */
public class LevCommand implements CommandExecutor, TabCompleter {

    private static final List<String> FEATURES = List.of(
            "nametags", "pearl", "enderchest",
            "freecam", "damagecap",
            "lightning", "mimic", "killeffect", "envoy"
    );

    private final ExtractionEventPlugin plugin;

    public LevCommand(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Command execution ─────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!player.hasPermission("extractionevent.admin")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            openGui(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "gui"           -> openGui(player);
            case "wand"          -> giveWand(player);
            case "create"        -> handleCreate(player, args);
            case "remove"        -> handleRemove(player, args);
            case "list"          -> handleList(player);
            case "reload"        -> plugin.reload(player);
            case "testmode"      -> handleTestMode(player);
            case "shownametags",
                 "showNametags",
                 "nametags"      -> handleShowNameTags(player);
            case "toggle"        -> handleToggle(player, args);
            case "shift"         -> handleShift(player, args);
            default              -> sendHelp(player);
        }
        return true;
    }

    // ── Sub-command handlers ──────────────────────────────────────────────────

    private void openGui(Player player) {
        new com.criztiandev.extractionevent.gui.MainMenuGui(plugin).open(player);
    }

    private void giveWand(Player player) {
        player.getInventory().addItem(WandUtil.getWand(plugin));
        player.sendMessage("§aYou have received the Lev Region wand.");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage("§cUsage: §e/lev create <name>"); return; }
        String name = args[1];
        if (plugin.getRegionManager().getRegion(name) != null) {
            player.sendMessage("§cA region named §e" + name + " §calready exists.");
            return;
        }
        var selection = plugin.getRegionManager().getSelection(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            player.sendMessage("§cMake a complete selection with the wand first.");
            return;
        }
        LevRegion region = new LevRegion(name, selection.getWorldName(),
                selection.getMinX(), selection.getMaxX(),
                selection.getMinZ(), selection.getMaxZ());
        // Let LevRegion defaults (-64 to 320) apply so the region is full-height.
        plugin.getRegionManager().saveRegion(region);
        plugin.getRegionManager().removeSelection(player.getUniqueId());
        player.sendMessage("§aRegion §e" + name + " §acreated.");
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage("§cUsage: §e/lev remove <name>"); return; }
        String name = args[1];
        if (plugin.getRegionManager().getRegion(name) == null) {
            player.sendMessage("§cNo region named §e" + name + "§c.");
            return;
        }
        plugin.getRegionManager().deleteRegion(name);
        player.sendMessage("§aRegion §e" + name + " §adeleted.");
    }

    private void handleList(Player player) {
        var regions = plugin.getRegionManager().getRegions();
        player.sendMessage("§d--- Lev Regions (" + regions.size() + ") ---");
        for (LevRegion r : regions) {
            player.sendMessage("§e" + r.getId() + " §7(" + r.getWorld() + ")");
        }
    }

    private void handleTestMode(Player player) {
        boolean now = plugin.toggleTestMode(player.getUniqueId());
        String state = now ? "§aON §7— restrictions apply to you" : "§cOFF §7— you bypass restrictions";
        player.sendMessage("§d[Lev] §7Test mode: " + state);
    }

    /**
     * /lev showNameTags
     * Toggles server-wide reveal mode: when ON, all warzone players' real names are
     * visible in the tab list and chat (the overhead nametag remains hidden by the team).
     * Useful for admins to identify players mid-fight without breaking immersion for others.
     */
    private void handleShowNameTags(Player player) {
        boolean nowRevealing = plugin.getNameTagManager().toggleRevealMode();
        String state = nowRevealing ? "§aON §7— real names now visible in tab list" : "§cOFF §7— all players are Anonymous";
        player.sendMessage("§d[Lev] §7Name tag reveal mode: " + state);
        plugin.getLogger().info("[Lev] Admin " + player.getName() + " toggled name reveal: " + nowRevealing);
    }

    /**
     * /lev toggle <region> <feature> [on|off]
     */
    private void handleToggle(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: §e/lev toggle <region> <feature> [on|off]");
            player.sendMessage("§7Features: §e" + String.join(", ", FEATURES));
            return;
        }

        LevRegion region = plugin.getRegionManager().getRegion(args[1]);
        if (region == null) {
            player.sendMessage("§cNo region named §e" + args[1] + "§c.");
            return;
        }

        String feature = args[2].toLowerCase(Locale.ROOT);
        // Determine the desired value (true/false/flip)
        Boolean desired = null;
        if (args.length >= 4) {
            String val = args[3].toLowerCase(Locale.ROOT);
            if (val.equals("on") || val.equals("true"))  desired = true;
            if (val.equals("off") || val.equals("false")) desired = false;
        }

        boolean set = applyToggle(region, feature, desired);
        plugin.getRegionManager().saveRegion(region);

        String stateStr = set ? "§aEnabled" : "§cDisabled";
        player.sendMessage("§d[Lev] §e" + region.getId() + "§7 — §f" + feature + " §7→ " + stateStr);
    }

    private void handleShift(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: §e/lrev shift <region> <start|stop|settime> [duration_seconds]");
            return;
        }

        LevRegion region = plugin.getRegionManager().getRegion(args[1]);
        if (region == null) {
            player.sendMessage("§cNo region named §e" + args[1] + "§c.");
            return;
        }

        String action = args[2].toLowerCase(Locale.ROOT);
        if (action.equals("start")) {
            long duration = plugin.getConfig().getLong("warzone.warzone-shift-duration", 3600);
            if (args.length >= 4) {
                try {
                    duration = Long.parseLong(args[3]);
                } catch (NumberFormatException ignored) {}
            }
            plugin.getWarzoneShiftManager().startShift(region.getId(), duration);
            player.sendMessage("§aStarted a Warzone Shift in §e" + region.getId() + "§a for " + duration + "s.");
        } else if (action.equals("stop")) {
            if (plugin.getWarzoneShiftManager().stopShift(region.getId())) {
                player.sendMessage("§aStopped the active Warzone Shift in §e" + region.getId());
            } else {
                player.sendMessage("§cNo active shift in §e" + region.getId());
            }
        } else if (action.equals("settime")) {
            if (args.length < 4) {
                player.sendMessage("§cUsage: §e/lrev shift <region> settime <duration_seconds>");
                return;
            }
            try {
                long duration = Long.parseLong(args[3]);
                plugin.getWarzoneShiftManager().startShift(region.getId(), duration); // overrides
                player.sendMessage("§aUpdated Warzone Shift in §e" + region.getId() + "§a to " + duration + "s.");
            } catch (NumberFormatException e) {
                player.sendMessage("§cDuration must be a number!");
            }
        } else {
            player.sendMessage("§cUnknown action §e" + action + "§c. Use start, stop, or settime.");
        }
    }

    /** Applies the toggle to the correct field. Returns the new boolean state. */
    private boolean applyToggle(LevRegion region, String feature, Boolean desired) {
        return switch (feature) {
            case "nametags"    -> { region.setHideNameTags(desired != null ? desired : !region.isHideNameTags()); yield region.isHideNameTags(); }
            case "pearl"       -> { region.setBlockEnderPearl(desired != null ? desired : !region.isBlockEnderPearl()); yield region.isBlockEnderPearl(); }
            case "enderchest"  -> { region.setEnderChestRestricted(desired != null ? desired : !region.isEnderChestRestricted()); yield region.isEnderChestRestricted(); }
            case "freecam"     -> { region.setFreeCamBlocked(desired != null ? desired : !region.isFreeCamBlocked()); yield region.isFreeCamBlocked(); }
            case "damagecap"   -> { region.setDamageCapped(desired != null ? desired : !region.isDamageCapped()); yield region.isDamageCapped(); }
            case "lightning"   -> { region.setLightningOnDeath(desired != null ? desired : !region.isLightningOnDeath()); yield region.isLightningOnDeath(); }
            case "mimic"       -> { region.setSpawnMimic(desired != null ? desired : !region.isSpawnMimic()); yield region.isSpawnMimic(); }
            case "killeffect"  -> { region.setKillEffectEnabled(desired != null ? desired : !region.isKillEffectEnabled()); yield region.isKillEffectEnabled(); }
            case "envoy"       -> { region.setEnvoyEventEnabled(desired != null ? desired : !region.isEnvoyEventEnabled()); yield region.isEnvoyEventEnabled(); }
            default            -> false;
        };
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("extractionevent.admin")) return List.of();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                    "gui", "wand", "create", "remove", "list",
                    "testmode", "showNameTags", "toggle", "reload", "shift"
            ));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("remove") || sub.equals("toggle") || sub.equals("shift")) {
                // Region names
                for (LevRegion r : plugin.getRegionManager().getRegions()) {
                    completions.add(r.getId());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("toggle")) {
            completions.addAll(FEATURES);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("shift")) {
            completions.addAll(Arrays.asList("start", "stop", "settime"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("toggle")) {
            completions.addAll(Arrays.asList("on", "off"));
        }

        // Filter by what the player has typed so far
        String partial = args[args.length - 1].toLowerCase(Locale.ROOT);
        return completions.stream()
                .filter(c -> c.toLowerCase(Locale.ROOT).startsWith(partial))
                .collect(Collectors.toList());
    }

    // ── Help ──────────────────────────────────────────────────────────────────

    private void sendHelp(Player player) {
        player.sendMessage("§d--- Lev Commands ---");
        player.sendMessage("§e/lev §7— Open region GUI");
        player.sendMessage("§e/lev wand §7— Get selection wand");
        player.sendMessage("§e/lev create <name> §7— Create region from selection");
        player.sendMessage("§e/lev remove <name> §7— Delete a region");
        player.sendMessage("§e/lev list §7— List all regions");
        player.sendMessage("§e/lev reload §7— Reload config and regions (Plugman-safe)");
        player.sendMessage("§e/lev showNameTags §7— Toggle admin name-reveal mode");
        player.sendMessage("§e/lev testmode §7— Toggle test mode (bypass restrictions for yourself)");
        player.sendMessage("§e/lev shift <region> <start|stop|settime> §7— Manual Legend Event controls");
        player.sendMessage("§e/lev toggle <region> <feature> [on|off] §7— Toggle a region feature");
        player.sendMessage("§7Features: §e" + String.join(", ", FEATURES));
    }
}
