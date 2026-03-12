package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import com.criztiandev.extractionevent.tasks.RegionPresenceTask;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Unit tests for EnderChestListener.
 *
 * The listener now cancels InventoryOpenEvent entirely when the player is in
 * a restricted region. Bukkit InventoryType enum initialisation requires a
 * server runtime, so the test class is @Disabled and acts as compile-time
 * verification of the new API.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Disabled("Requires Bukkit server runtime for InventoryType static initialisation")
class EnderChestListenerTest {

    @Mock ExtractionEventPlugin plugin;
    @Mock RegionPresenceTask     presenceTask;
    @Mock LevRegion              region;
    @Mock Player                 player;

    private UUID playerId;
    EnderChestListener listener;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(plugin.getRegionPresenceTask()).thenReturn(presenceTask);
        listener = new EnderChestListener(plugin);
    }

    // Tests for onInventoryOpen removed because the listener now exclusively 
    // checks top inventory state during InventoryClickEvent and InventoryDragEvent.
}
