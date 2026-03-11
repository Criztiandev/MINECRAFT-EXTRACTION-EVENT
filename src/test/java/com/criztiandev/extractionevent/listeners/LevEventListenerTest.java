package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.managers.RegionManager;
import com.criztiandev.extractionevent.models.LevRegion;
import com.criztiandev.extractionevent.tasks.RegionPresenceTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Unit tests for LevEventListener (Bug 4 — anti-stasis, ender pearl blocking).
 *
 * TeleportCause is accessed via mock — avoids static class initializer issues
 * in a test environment without a CraftBukkit server. The listener only uses
 * TeleportCause comparisons, which work fine on the mocked enum values.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LevEventListenerTest {

    @Mock ExtractionEventPlugin plugin;
    @Mock RegionManager         regionManager;
    @Mock RegionPresenceTask    presenceTask;
    @Mock Player                player;
    @Mock LevRegion             region;

    private UUID playerId;
    LevEventListener listener;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(plugin.getRegionManager()).thenReturn(regionManager);
        when(plugin.getRegionPresenceTask()).thenReturn(presenceTask);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        listener = new LevEventListener(plugin);
    }

    private PlayerTeleportEvent mockTeleport(
            PlayerTeleportEvent.TeleportCause cause, Location from, Location to) {
        PlayerTeleportEvent event = mock(PlayerTeleportEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getCause()).thenReturn(cause);
        when(event.getFrom()).thenReturn(from);
        when(event.getTo()).thenReturn(to);
        when(event.isCancelled()).thenReturn(false);
        return event;
    }

    // ── Ender Pearl restriction ───────────────────────────────────────────────

    @Test
    void enderPearlOutOfRegionWithBlockEnabled_shouldBeCancelled() {
        Location inside  = mock(Location.class);
        Location outside = mock(Location.class);

        // Always use precise spatial scan (no cache shortcut for pearls)
        when(regionManager.getRegionAt(inside)).thenReturn(region);
        when(regionManager.getRegionAt(outside)).thenReturn(null);
        when(region.isBlockEnderPearl()).thenReturn(true);
        when(region.getId()).thenReturn("test-region");

        PlayerTeleportEvent event = mockTeleport(
                PlayerTeleportEvent.TeleportCause.ENDER_PEARL, inside, outside);
        listener.onPlayerTeleport(event);

        verify(event).setCancelled(true);
    }

    @Test
    void enderPearlIntoRegionFromOutside_shouldBeCancelled() {
        Location outside = mock(Location.class);
        Location inside  = mock(Location.class);

        when(regionManager.getRegionAt(outside)).thenReturn(null);
        when(regionManager.getRegionAt(inside)).thenReturn(region);
        when(region.isBlockEnderPearl()).thenReturn(true);
        when(region.getId()).thenReturn("test-region");

        PlayerTeleportEvent event = mockTeleport(
                PlayerTeleportEvent.TeleportCause.ENDER_PEARL, outside, inside);
        listener.onPlayerTeleport(event);

        verify(event).setCancelled(true);
    }

    @Test
    void enderPearlWhileOutsideAllRegions_shouldNotBeCancelled() {
        Location from = mock(Location.class);
        Location to   = mock(Location.class);
        when(regionManager.getRegionAt(from)).thenReturn(null);
        when(regionManager.getRegionAt(to)).thenReturn(null);

        PlayerTeleportEvent event = mockTeleport(
                PlayerTeleportEvent.TeleportCause.ENDER_PEARL, from, to);
        listener.onPlayerTeleport(event);

        verify(event, never()).setCancelled(true);
    }
}
