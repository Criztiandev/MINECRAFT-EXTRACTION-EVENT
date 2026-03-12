package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.managers.RegionManager;
import com.criztiandev.extractionevent.managers.WarzoneShiftManager;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.mockito.Mockito.*;

class EnvoyEventListenerTest {

    @Mock
    private ExtractionEventPlugin plugin;
    @Mock
    private WarzoneShiftManager shiftManager;
    @Mock
    private RegionManager regionManager;
    @Mock
    private Player player;
    @Mock
    private Location loc;
    
    // A dummy event representing AdvancedEnchantments API to allow reflection logic to succeed
    private static class DummyAEEvent extends Event {
        private final Player p;
        private boolean cancelled = false;

        public DummyAEEvent(Player p) { this.p = p; }
        public Player getPlayer() { return p; }
        public void setCancelled(boolean val) { this.cancelled = val; }
        public boolean isCancelled() { return cancelled; }

        @Override
        public HandlerList getHandlers() { return new HandlerList(); }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getWarzoneShiftManager()).thenReturn(shiftManager);
        when(plugin.getRegionManager()).thenReturn(regionManager);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        when(player.getLocation()).thenReturn(loc);
    }

    @Test
    void testCancelWhenShiftActive() throws Exception {
        LevRegion region = new LevRegion("test_zone", "world", 0, 10, 0, 10);
        region.setEnvoyEventEnabled(false); // Explicit toggle is OFF

        // But global shift IS active!
        when(shiftManager.isAnyShiftActive()).thenReturn(true);
        when(shiftManager.isShiftActive("test_zone")).thenReturn(true);
        when(regionManager.getRegionAt(loc)).thenReturn(region);

        try (MockedStatic<org.bukkit.Bukkit> mockedBukkit = mockStatic(org.bukkit.Bukkit.class)) {
            org.bukkit.plugin.PluginManager pm = mock(org.bukkit.plugin.PluginManager.class);
            mockedBukkit.when(org.bukkit.Bukkit::getPluginManager).thenReturn(pm);

            // Setup a dummy event and listener instance
            EnvoyEventListener listener = new EnvoyEventListener(plugin);

            // Inject the event directly targeting the reflection method
            Method onAE = EnvoyEventListener.class.getDeclaredMethod("onAEEvent", Event.class);
            onAE.setAccessible(true);
        
            DummyAEEvent event = new DummyAEEvent(player);
            onAE.invoke(listener, event);

            // Assert that reflection setCancelled(true) worked!
            assert(event.isCancelled());
        }
    }

    @Test
    void testCancelWhenRegionExplicit() throws Exception {
        LevRegion region = new LevRegion("explicit_zone", "world", 0, 10, 0, 10);
        region.setEnvoyEventEnabled(true); // Admin explicitly turned it OFF permanently for this zone

        when(shiftManager.isAnyShiftActive()).thenReturn(false); // Global shift inactive
        when(regionManager.getRegions()).thenReturn(Collections.singletonList(region));
        when(regionManager.getRegionAt(loc)).thenReturn(region);

        try (MockedStatic<org.bukkit.Bukkit> mockedBukkit = mockStatic(org.bukkit.Bukkit.class)) {
            org.bukkit.plugin.PluginManager pm = mock(org.bukkit.plugin.PluginManager.class);
            mockedBukkit.when(org.bukkit.Bukkit::getPluginManager).thenReturn(pm);

            EnvoyEventListener listener = new EnvoyEventListener(plugin);

            Method onAE = EnvoyEventListener.class.getDeclaredMethod("onAEEvent", Event.class);
            onAE.setAccessible(true);
        
            DummyAEEvent event = new DummyAEEvent(player);
            onAE.invoke(listener, event);

            // Assert that reflection setCancelled(true) worked!
            assert(event.isCancelled());
        }
    }

    @Test
    void testBypassWhenNeitherActive() throws Exception {
        LevRegion region = new LevRegion("normal_zone", "world", 0, 10, 0, 10);
        region.setEnvoyEventEnabled(false); // Admin allowed enchants here

        when(shiftManager.isAnyShiftActive()).thenReturn(false); // Global shift inactive
        when(regionManager.getRegions()).thenReturn(Collections.singletonList(region));
        when(regionManager.getRegionAt(loc)).thenReturn(region);

        try (MockedStatic<org.bukkit.Bukkit> mockedBukkit = mockStatic(org.bukkit.Bukkit.class)) {
            org.bukkit.plugin.PluginManager pm = mock(org.bukkit.plugin.PluginManager.class);
            mockedBukkit.when(org.bukkit.Bukkit::getPluginManager).thenReturn(pm);

            EnvoyEventListener listener = new EnvoyEventListener(plugin);

            Method onAE = EnvoyEventListener.class.getDeclaredMethod("onAEEvent", Event.class);
            onAE.setAccessible(true);
        
            DummyAEEvent event = new DummyAEEvent(player);
            onAE.invoke(listener, event);

            // Assert that the event was ignored and NOT cancelled!
            assert(!event.isCancelled());
        }
    }
}
