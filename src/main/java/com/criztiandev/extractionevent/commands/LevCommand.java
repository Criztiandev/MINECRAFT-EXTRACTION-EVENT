package com.criztiandev.extractionevent.commands;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import com.criztiandev.extractionevent.models.RegionSelection;
import com.criztiandev.extractionevent.utils.WandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LevCommand implements CommandExecutor {

    private final ExtractionEventPlugin plugin;

    public LevCommand(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("extractionevent.admin")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            new com.criztiandev.extractionevent.gui.MainMenuGui(plugin).open(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "gui":
            case "admin":
                new com.criztiandev.extractionevent.gui.MainMenuGui(plugin).open(player);
                break;
            case "wand":
                player.getInventory().addItem(WandUtil.getWand(plugin));
                player.sendMessage("§aYou have received the Lev Region wand.");
                break;
            case "create":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /lev create <name>");
                    return true;
                }
                String name = args[1];
                handleCreate(player, name);
                break;
            case "remove":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /lev remove <name>");
                    return true;
                }
                String removeName = args[1];
                handleRemove(player, removeName);
                break;
            case "list":
                handleList(player);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String name) {
        if (plugin.getRegionManager().getRegion(name) != null) {
            player.sendMessage("§cThere is already a region named " + name + "!");
            return;
        }

        RegionSelection selection = plugin.getRegionManager().getSelection(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            player.sendMessage("§cYou must first make a complete selection using the lev wand.");
            return;
        }

        LevRegion region = new LevRegion(
                name,
                selection.getWorldName(),
                selection.getMinX(),
                selection.getMaxX(),
                selection.getMinZ(),
                selection.getMaxZ()
        );
        region.setMinY(selection.getMinY());
        region.setMaxY(selection.getMaxY());

        plugin.getRegionManager().saveRegion(region);
        plugin.getRegionManager().removeSelection(player.getUniqueId());
        player.sendMessage("§aLev Region '" + name + "' successfully created.");
    }

    private void handleRemove(Player player, String name) {
        if (plugin.getRegionManager().getRegion(name) == null) {
            player.sendMessage("§cRegion '" + name + "' does not exist.");
            return;
        }
        plugin.getRegionManager().deleteRegion(name);
        player.sendMessage("§aLev Region '" + name + "' successfully removed.");
    }

    private void handleList(Player player) {
        player.sendMessage("§d--- Active Lev Regions ---");
        for (LevRegion region : plugin.getRegionManager().getRegions()) {
            player.sendMessage("§e- " + region.getId() + " §7(" + region.getWorld() + ")");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§d--- Lev Commands ---");
        player.sendMessage("§e/lev wand §7- Get the selection wand");
        player.sendMessage("§e/lev create <name> §7- Create region from selection");
        player.sendMessage("§e/lev remove <name> §7- Delete a region");
        player.sendMessage("§e/lev list §7- List all regions");
    }
}
