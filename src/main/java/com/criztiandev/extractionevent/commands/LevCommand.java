package com.criztiandev.extractionevent.commands;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.managers.AdminMonitorManager;
import com.criztiandev.extractionevent.managers.LockdownManager;
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
 * /lev  (aliases: /lrev, /ee, /extractionevent)
 *
 * ── Region management ─────────────────────────────────────────────
 *   /lev                           → opens the admin GUI
 *   /lev wand                      → gives selection wand
 *   /lev create <name>             → creates region from wand selection
 *   /lev remove <name>             → deletes a region
 *   /lev list                      → lists all regions
 *   /lev feature <region> <flag> [on|off]  → toggles a per-region setting
 *
 * ── Event controls ────────────────────────────────────────────────
 *   /lev event <region> start [seconds]  → start a Warzone Shift
 *   /lev event <region> stop             → stop an active Warzone Shift
 *
 * ── Admin / utilities ─────────────────────────────────────────────
 *   /lev monitor <nametags|chat|map>     → toggle your personal monitor view
 *   /lev testmode                        → let restrictions apply to yourself
 *   /lev reload                          → hot-reload config & regions
 */
public class LevCommand implements CommandExecutor, TabCompleter {

    private static final List<String> REGION_FEATURES = List.of(
            "nametags", "pearl", "enderchest",
            "freecam", "damagecap",
            "lightning", "mimic", "killeffect", "envoy"
    );

    private static final List<String> MONITOR_FEATURES = List.of("nametags", "chat", "map");

    private final ExtractionEventPlugin plugin;

    public LevCommand(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can run this command.");
            return true;
        }
        if (!player.hasPermission("extractionevent.admin")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            openGui(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "wand"     -> giveWand(player);
            case "create"   -> handleCreate(player, args);
            case "remove"   -> handleRemove(player, args);
            case "list"     -> handleList(player);
            case "feature"  -> handleFeature(player, args);
            case "event"    -> handleEvent(player, args);
            case "lockdown" -> handleLockdown(player, args);
            case "koth"     -> handleKoth(player, args);
            case "monitor"  -> handleMonitor(player, args);
            case "testmode" -> handleTestMode(player);
            case "reload"   -> plugin.reload(player);
            default         -> sendHelp(player);
        }
        return true;
    }

    // ── Region management ─────────────────────────────────────────────────────

    private void openGui(Player player) {
        new com.criztiandev.extractionevent.gui.MainMenuGui(plugin).open(player);
    }

    private void giveWand(Player player) {
        player.getInventory().addItem(WandUtil.getWand(plugin));
        player.sendMessage("§a✔ Wand added to your inventory. Left/right-click blocks to select corners.");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: §e/lev create <name>");
            player.sendMessage("§7Tip: Make a wand selection first with §e/lev wand");
            return;
        }
        String name = args[1];
        if (plugin.getRegionManager().getRegion(name) != null) {
            player.sendMessage("§c✘ A region called §e'" + name + "'§c already exists. Use a different name.");
            return;
        }
        var selection = plugin.getRegionManager().getSelection(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            player.sendMessage("§c✘ No complete selection found. Use §e/lev wand §cto select two corners first.");
            return;
        }
        LevRegion region = new LevRegion(name, selection.getWorldName(),
                selection.getMinX(), selection.getMaxX(),
                selection.getMinZ(), selection.getMaxZ());
        plugin.getRegionManager().saveRegion(region);
        plugin.getRegionManager().removeSelection(player.getUniqueId());
        player.sendMessage("§a✔ Region §e'" + name + "'§a created.");
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: §e/lev remove <region-name>");
            return;
        }
        String name = args[1];
        if (plugin.getRegionManager().getRegion(name) == null) {
            player.sendMessage("§c✘ No region called §e'" + name + "'§c. Check §e/lev list§c for names.");
            return;
        }
        plugin.getRegionManager().deleteRegion(name);
        player.sendMessage("§a✔ Region §e'" + name + "'§a deleted.");
    }

    private void handleList(Player player) {
        var regions = plugin.getRegionManager().getRegions();
        if (regions.isEmpty()) {
            player.sendMessage("§7No regions created yet. Use §e/lev wand §7+ §e/lev create <name>§7.");
            return;
        }
        player.sendMessage("§d§l── Regions (" + regions.size() + ") ──────────────────");
        for (LevRegion r : regions) {
            player.sendMessage("§e" + r.getId() + " §8• §7" + r.getWorld());
        }
    }

    // ── Feature flags ─────────────────────────────────────────────────────────

    /**
     * /lev feature <region> <flag> [on|off]
     * Former name: toggle — renamed to avoid confusion with monitor toggles.
     */
    private void handleFeature(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: §e/lev feature <region> <flag> [on|off]");
            player.sendMessage("§7Flags: §e" + String.join("§7, §e", REGION_FEATURES));
            return;
        }

        LevRegion region = plugin.getRegionManager().getRegion(args[1]);
        if (region == null) {
            player.sendMessage("§c✘ No region called §e'" + args[1] + "'§c. Check §e/lev list§c.");
            return;
        }

        String flag = args[2].toLowerCase(Locale.ROOT);
        Boolean desired = null;
        if (args.length >= 4) {
            String val = args[3].toLowerCase(Locale.ROOT);
            if (val.equals("on")  || val.equals("true"))  desired = true;
            if (val.equals("off") || val.equals("false")) desired = false;
        }

        boolean set = applyFeature(player, region, flag, desired);
        plugin.getRegionManager().saveRegion(region);

        String stateStr = set ? "§aEnabled ✔" : "§cDisabled ✘";
        player.sendMessage("§d[Feature] §e" + region.getId() + " §8» §f" + flag + " §8→ " + stateStr);
    }

    private boolean applyFeature(Player player, LevRegion region, String flag, Boolean desired) {
        return switch (flag) {
            case "nametags"   -> { region.setHideNameTags(desired != null ? desired : !region.isHideNameTags()); yield region.isHideNameTags(); }
            case "pearl"      -> { region.setBlockEnderPearl(desired != null ? desired : !region.isBlockEnderPearl()); yield region.isBlockEnderPearl(); }
            case "enderchest" -> { region.setEnderChestRestricted(desired != null ? desired : !region.isEnderChestRestricted()); yield region.isEnderChestRestricted(); }
            case "freecam"    -> { region.setFreeCamBlocked(desired != null ? desired : !region.isFreeCamBlocked()); yield region.isFreeCamBlocked(); }
            case "damagecap"  -> { region.setDamageCapped(desired != null ? desired : !region.isDamageCapped()); yield region.isDamageCapped(); }
            case "lightning"  -> { region.setLightningOnDeath(desired != null ? desired : !region.isLightningOnDeath()); yield region.isLightningOnDeath(); }
            case "mimic"      -> { region.setSpawnMimic(desired != null ? desired : !region.isSpawnMimic()); yield region.isSpawnMimic(); }
            case "killeffect" -> { region.setKillEffectEnabled(desired != null ? desired : !region.isKillEffectEnabled()); yield region.isKillEffectEnabled(); }
            case "envoy"      -> { region.setEnvoyEventEnabled(desired != null ? desired : !region.isEnvoyEventEnabled()); yield region.isEnvoyEventEnabled(); }
            default           -> {
                player.sendMessage("§c✘ Unknown flag §e'" + flag + "'§c.");
                player.sendMessage("§7Valid flags: §e" + String.join("§7, §e", REGION_FEATURES));
                yield false;
            }
        };
    }

    // ── Lockdown / KOTH ───────────────────────────────────────────────────────

    /**
     * /lev lockdown start  — lock every extraction region instantly
     * /lev lockdown stop   — re-open all extraction regions
     * /lev lockdown status — show current lockdown state
     */
    private void handleLockdown(Player player, String[] args) {
        LockdownManager lm = plugin.getLockdownManager();

        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            if (lm.isLockdownActive()) {
                player.sendMessage("§8[§4⚠ LOCKDOWN§8] §cAll extraction points are §4LOCKED§c.");
            } else if (lm.isKothActive()) {
                player.sendMessage("§8[§6⚔ KOTH§8] §eKOTH active — only §a" + lm.getKothRegionId() + "§e is open.");
            } else {
                player.sendMessage("§8[§aLockdown§8] §7No lockdown active. All extraction points are §aopen§7.");
            }
            return;
        }

        switch (args[1].toLowerCase()) {
            case "start" -> {
                if (lm.isLockdownActive()) {
                    player.sendMessage("§cLockdown is already active. Use §e/lev lockdown stop §cfirst.");
                    return;
                }
                if (!lm.startLockdown(player)) {
                    player.sendMessage("§c✘ Failed — is ExtractionRegionEditor loaded?");
                }
            }
            case "stop" -> {
                if (!lm.isLockdownActive() && !lm.isKothActive()) {
                    player.sendMessage("§7No lockdown is currently active.");
                    return;
                }
                if (!lm.stop(player)) {
                    player.sendMessage("§c✘ Failed — is ExtractionRegionEditor loaded?");
                }
            }
            default -> player.sendMessage("§cUsage: §e/lev lockdown <start|stop|status>");
        }
    }

    /**
     * /lev koth <region> start  — lock all regions except <region>
     * /lev koth stop            — end KOTH (re-opens all regions)
     */
    private void handleKoth(Player player, String[] args) {
        LockdownManager lm = plugin.getLockdownManager();

        if (args.length < 2) {
            player.sendMessage("§cUsage:");
            player.sendMessage("§e  /lev koth <region> start §8— §7lock all regions except <region>");
            player.sendMessage("§e  /lev koth stop           §8— §7end KOTH mode, open all regions");
            if (lm.isKothActive()) {
                player.sendMessage("§8Status: §6KOTH active §8— king region: §a" + lm.getKothRegionId());
            }
            return;
        }

        // /lev koth stop
        if (args[1].equalsIgnoreCase("stop")) {
            if (!lm.isKothActive()) {
                player.sendMessage("§7KOTH is not currently active.");
                return;
            }
            if (!lm.stop(player)) {
                player.sendMessage("§c✘ Failed — is ExtractionRegionEditor loaded?");
            }
            return;
        }

        // /lev koth <region> start
        if (args.length < 3 || !args[2].equalsIgnoreCase("start")) {
            player.sendMessage("§cUsage: §e/lev koth <region> start§c  or  §e/lev koth stop");
            return;
        }

        String regionId = args[1];
        if (lm.isKothActive()) {
            player.sendMessage("§cKOTH is already active (king: §e" + lm.getKothRegionId() + "§c). Stop it first with §e/lev koth stop§c.");
            return;
        }
        if (!lm.startKoth(player, regionId)) {
            player.sendMessage("§c✘ Failed — region §e'" + regionId + "'§c not found or ExtractionRegionEditor is not loaded.");
        }
    }

    // ── Event (Warzone Shift) ─────────────────────────────────────────────────

    /**
     * /lev event <region> start [seconds]
     * /lev event <region> stop
     */
    private void handleEvent(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage:");
            player.sendMessage("§e  /lev event <region> start [seconds] §7— begin a Warzone Shift");
            player.sendMessage("§e  /lev event <region> stop             §7— end the current shift");
            return;
        }

        LevRegion region = plugin.getRegionManager().getRegion(args[1]);
        if (region == null) {
            player.sendMessage("§c✘ No region called §e'" + args[1] + "'§c. Check §e/lev list§c.");
            return;
        }

        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "start" -> {
                long duration = plugin.getConfig().getLong("warzone.warzone-shift-duration", 3600);
                if (args.length >= 4) {
                    try { duration = Long.parseLong(args[3]); }
                    catch (NumberFormatException e) {
                        player.sendMessage("§c✘ Duration must be a number (seconds). Example: §e/lev event " + region.getId() + " start 1800");
                        return;
                    }
                }
                plugin.getWarzoneShiftManager().startShift(region.getId(), duration);
                player.sendMessage("§a✔ Warzone Shift started in §e'" + region.getId() + "'§a for §e" + duration + "s§a.");
            }
            case "stop" -> {
                if (plugin.getWarzoneShiftManager().stopShift(region.getId())) {
                    player.sendMessage("§a✔ Warzone Shift stopped in §e'" + region.getId() + "'§a.");
                } else {
                    player.sendMessage("§c✘ No active shift in §e'" + region.getId() + "'§c.");
                }
            }
            default -> {
                player.sendMessage("§c✘ Unknown action §e'" + action + "'§c. Use §estart§c or §estop§c.");
            }
        }
    }

    // ── Admin monitor ─────────────────────────────────────────────────────────

    /**
     * /lev monitor <nametags|chat|map>
     * Per-admin toggle — each admin independently chooses what they see.
     */
    private void handleMonitor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§d§l── Monitor Toggles (your personal view) ──");
            AdminMonitorManager monitor = plugin.getAdminMonitorManager();
            player.sendMessage(statusLine(monitor, player, AdminMonitorManager.Feature.NAMETAGS, "nametags", "see real names above heads"));
            player.sendMessage(statusLine(monitor, player, AdminMonitorManager.Feature.CHAT,     "chat",     "see [Monitor] RealName in chat"));
            player.sendMessage(statusLine(monitor, player, AdminMonitorManager.Feature.MAP,      "map",      "keep your minimap in the warzone"));
            player.sendMessage("§8Run §e/lev monitor <feature>§8 to toggle.");
            return;
        }

        AdminMonitorManager.Feature feature = AdminMonitorManager.Feature.fromString(args[1]);
        if (feature == null) {
            player.sendMessage("§c✘ Unknown feature §e'" + args[1] + "'§c. Choose: §enametags§c, §echat§c, §emap§c.");
            return;
        }

        boolean now = plugin.getAdminMonitorManager().toggle(player.getUniqueId(), feature);
        String state = now ? "§aON  ✔" : "§cOFF ✘";
        player.sendMessage("§d[Monitor] §f" + args[1].toLowerCase() + " §8→ " + state);
    }

    private String statusLine(AdminMonitorManager m, Player p, AdminMonitorManager.Feature f, String name, String desc) {
        String state = m.has(p, f) ? "§aON §8 " : "§cOFF§8";
        return "§7  [" + state + "§8] §e" + name + " §8— §7" + desc;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void handleTestMode(Player player) {
        boolean now = plugin.toggleTestMode(player.getUniqueId());
        if (now) {
            player.sendMessage("§a✔ Test mode ON §7— restrictions now apply to you (you play like a normal player).");
        } else {
            player.sendMessage("§c✘ Test mode OFF §7— you bypass warzone restrictions again.");
        }
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("extractionevent.admin")) return List.of();

        List<String> completions = new ArrayList<>();
        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 1) {
            completions.addAll(List.of(
                    "wand", "create", "remove", "list",
                    "feature", "event", "lockdown", "koth",
                    "monitor", "testmode", "reload"
            ));
        } else if (args.length == 2) {
            switch (sub) {
                case "remove", "feature", "event" -> plugin.getRegionManager().getRegions()
                        .forEach(r -> completions.add(r.getId()));
                case "monitor"   -> completions.addAll(MONITOR_FEATURES);
                case "lockdown"  -> completions.addAll(List.of("start", "stop", "status"));
                case "koth"      -> {
                    completions.add("stop");
                    plugin.getLockdownManager().getExtractionRegionIds()
                            .forEach(completions::add);
                }
            }
        } else if (args.length == 3) {
            switch (sub) {
                case "feature" -> completions.addAll(REGION_FEATURES);
                case "event"   -> completions.addAll(List.of("start", "stop"));
                case "koth"    -> completions.add("start");
            }
        } else if (args.length == 4 && sub.equals("feature")) {
            completions.addAll(List.of("on", "off"));
        } else if (args.length == 4 && sub.equals("event") && args[2].equalsIgnoreCase("start")) {
            completions.add("<seconds>");
        }

        String partial = args[args.length - 1].toLowerCase(Locale.ROOT);
        return completions.stream()
                .filter(c -> c.toLowerCase(Locale.ROOT).startsWith(partial))
                .collect(Collectors.toList());
    }

    // ── Help ──────────────────────────────────────────────────────────────────

    private void sendHelp(Player player) {
        player.sendMessage("§d§l── Lev Admin Commands ─────────────────────────");
        player.sendMessage("§8Region setup:");
        player.sendMessage("§e  /lev wand                    §8— §7get selection wand");
        player.sendMessage("§e  /lev create <name>           §8— §7create region from selection");
        player.sendMessage("§e  /lev remove <name>           §8— §7delete a region");
        player.sendMessage("§e  /lev list                    §8— §7list all regions");
        player.sendMessage("§e  /lev feature <region> <flag> §8— §7toggle a region flag");
        player.sendMessage("§8Event controls:");
        player.sendMessage("§e  /lev event <region> start [s] §8— §7start a Warzone Shift");
        player.sendMessage("§e  /lev event <region> stop      §8— §7stop the active shift");
        player.sendMessage("§8Lockdown / KOTH:");
        player.sendMessage("§e  /lev lockdown start|stop|status §8— §7lock all extraction points instantly");
        player.sendMessage("§e  /lev koth <region> start        §8— §7KOTH: only <region> allows extraction");
        player.sendMessage("§e  /lev koth stop                  §8— §7end KOTH mode");
        player.sendMessage("§8Admin monitor (personal):");
        player.sendMessage("§e  /lev monitor                 §8— §7show your monitor status");
        player.sendMessage("§e  /lev monitor <nametags|chat|map> §8— §7toggle a monitor view");
        player.sendMessage("§8Utilities:");
        player.sendMessage("§e  /lev testmode                §8— §7toggle restriction testing");
        player.sendMessage("§e  /lev reload                  §8— §7hot-reload config");
        player.sendMessage("§7Region flags: §e" + String.join(" §7| §e", REGION_FEATURES));
    }
}
