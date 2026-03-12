package com.criztiandev.extractionevent.managers;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WarzoneShiftManagerTest {

    @Mock
    private ExtractionEventPlugin plugin;
    @Mock
    private RegionManager regionManager;

    private WarzoneShiftManager manager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getRegionManager()).thenReturn(regionManager);
        // By returning null for getRegion(id), we intentionally cause `broadcastShiftState` 
        // to return early and avoid invoking static Bukkit methods.
        when(regionManager.getRegion(anyString())).thenReturn(null);

        manager = new WarzoneShiftManager(plugin);
    }

    @Test
    void testStartShift() {
        String regionId = "test_zone";
        manager.startShift(regionId, 3600); // 1 hour

        assertTrue(manager.isShiftActive(regionId), "Shift should be active immediately after starting.");
        assertTrue(manager.isAnyShiftActive(), "AnyShiftActive should be true.");
        assertTrue(manager.getRemainingSeconds(regionId) > 3500 && manager.getRemainingSeconds(regionId) <= 3600, "Remaining seconds should be nearly 3600.");
    }

    @Test
    void testStopShift() {
        String regionId = "test_zone";
        manager.startShift(regionId, 3600);
        
        boolean stopped = manager.stopShift(regionId);
        assertTrue(stopped, "Stopping an active shift should return true.");
        assertFalse(manager.isShiftActive(regionId), "Shift should no longer be active.");
        assertFalse(manager.isAnyShiftActive(), "AnyShiftActive should be false since it was the only shift.");
    }

    @Test
    void testStopNonExistentShift() {
        boolean stopped = manager.stopShift("unknown");
        assertFalse(stopped, "Stopping a non-existent shift should return false.");
    }

    @Test
    void testShiftExpiration() throws InterruptedException {
        String regionId = "fast_zone";
        manager.startShift(regionId, 1); // 1 second duration

        assertTrue(manager.isShiftActive(regionId), "Shift should be active initially.");
        
        // Wait for it to expire
        Thread.sleep(1100);

        // Verification relies on lazy evaluation inside isShiftActive
        assertFalse(manager.isShiftActive(regionId), "Shift should lazy-expire after duration passes.");
    }
}
