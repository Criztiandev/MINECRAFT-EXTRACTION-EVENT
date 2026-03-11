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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uses ProtocolLib to suppress PlayerInfo packets so players inside a warzone
 * region cannot be identified or tracked on minimaps (JourneyMap, Xaero's).
 *
 * When a player enters a region → send REMOVE info packets to all existing
 * region members (and vice-versa), making them invisible to each other's radar.
 * When a player leaves  a region → send ADD info packets to restore visibility.
 *
 * Combined with NameTagManager's §8Anonymous display-name trick, this gives
 * both structural (packet) and cosmetic (name) concealment.
 */
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
        for (Player member : getPlayersInRegion(region)) {
            if (member.getUniqueId().equals(entering.getUniqueId())) continue;
            sendRemovePacket(member, entering);
            sendRemovePacket(entering, member);
        }
    }

    /** Called when a player leaves a warzone region. */
    public void onPlayerLeaveRegion(Player leaving, LevRegion region) {
        for (Player member : getPlayersInRegion(region)) {
            if (member.getUniqueId().equals(leaving.getUniqueId())) continue;
            sendAddPacket(member, leaving);
            sendAddPacket(leaving, member);
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

    private List<Player> getPlayersInRegion(LevRegion region) {
        World world = Bukkit.getWorld(region.getWorld());
        if (world == null) return Collections.emptyList();

        List<Player> result = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            Location loc = p.getLocation();
            if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX()
                    && loc.getBlockY() >= region.getMinY() && loc.getBlockY() <= region.getMaxY()
                    && loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                result.add(p);
            }
        }
        return result;
    }

    private void logDebug(String msg) {
        if (debugEnabled) {
            plugin.getLogger().warning("[MinimapHide] " + msg);
        }
    }
}
