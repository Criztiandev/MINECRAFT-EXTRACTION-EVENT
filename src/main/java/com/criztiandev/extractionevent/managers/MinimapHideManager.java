package com.criztiandev.extractionevent.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class MinimapHideManager {

    private final ExtractionEventPlugin plugin;
    private final ProtocolManager protocolManager;
    private final boolean debugEnabled;

    public MinimapHideManager(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.debugEnabled = plugin.getConfig().getBoolean("settings.debug", false);
    }

    /** Called when a player enters a warzone region. */
    public void onPlayerEnterRegion(Player entering, LevRegion region) {
        entering.sendMessage("§3§6§8§9§e§f" + "§f§a§i§r§x§a§e§r§o" + "§n§o§m§i§n§i§m§a§p");

        // Tab-list hide from existing warzone members (mutual)
        for (Player member : plugin.getRegionPresenceTask().getCachedPlayersInRegion(region)) {
            if (member.getUniqueId().equals(entering.getUniqueId())) continue;
            sendRemovePacket(member, entering);
            sendRemovePacket(entering, member);
        }

        // Entity-level hide from ALL outside players so JourneyMap entity tracker hides the dot
        for (Player outsider : Bukkit.getOnlinePlayers()) {
            if (plugin.getRegionPresenceTask().isInAnyRegion(outsider.getUniqueId())) continue;
            outsider.hidePlayer(plugin, entering);
        }
    }

    /** Called when a player leaves a warzone region. */
    public void onPlayerLeaveRegion(Player leaving, LevRegion region) {
        leaving.sendMessage("§3§6§8§9§e§0" + "§r§e§s§e§t§x§a§e§r§o");

        // Restore tab-list for warzone members
        for (Player member : plugin.getRegionPresenceTask().getCachedPlayersInRegion(region)) {
            if (member.getUniqueId().equals(leaving.getUniqueId())) continue;
            sendAddPacket(member, leaving);
            sendAddPacket(leaving, member);
        }

        // Restore entity visibility to all outside players
        for (Player outsider : Bukkit.getOnlinePlayers()) {
            if (plugin.getRegionPresenceTask().isInAnyRegion(outsider.getUniqueId())) continue;
            outsider.showPlayer(plugin, leaving);
        }
    }

    // ── Packet helpers ────────────────────────────────────────────────────────

    /**
     * Sends a PLAYER_INFO_REMOVE packet to [recipient], hiding [target] from
     * their tab-list / minimap radar.
     */
    private void sendRemovePacket(Player recipient, Player target) {
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            packet.getUUIDLists().write(0, Collections.singletonList(target.getUniqueId()));
            protocolManager.sendServerPacket(recipient, packet);
        } catch (Exception e) {
            logDebug("Failed to send REMOVE packet to " + recipient.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Sends a PLAYER_INFO ADD_PLAYER packet to [recipient], restoring [target]
     * in their tab-list / minimap radar.
     */
    private void sendAddPacket(Player recipient, Player target) {
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            // Write the action — ProtocolLib 5.x uses an int-based enum accessor
            packet.getPlayerInfoAction().write(0, PlayerInfoAction.ADD_PLAYER);

            WrappedGameProfile profile = WrappedGameProfile.fromPlayer(target);
            PlayerInfoData data = new PlayerInfoData(
                    profile,
                    target.getPing(),
                    NativeGameMode.fromBukkit(target.getGameMode()),
                    WrappedChatComponent.fromText(target.getPlayerListName())
            );
            packet.getPlayerInfoDataLists().write(1, Collections.singletonList(data));
            protocolManager.sendServerPacket(recipient, packet);
        } catch (Exception e) {
            logDebug("Failed to send ADD packet to " + recipient.getName() + ": " + e.getMessage());
        }
    }

    private void logDebug(String msg) {
        if (debugEnabled) {
            plugin.getLogger().warning("[MinimapHide] " + msg);
        }
    }
}
