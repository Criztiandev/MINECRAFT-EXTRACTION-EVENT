package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import com.criztiandev.extractionevent.tasks.RegionPresenceTask;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
 * Unit tests for DamageCapListener (Bug 5 — survival floor, Bug 6 — damage cap).
 *
 * Uses fully-mocked Bukkit objects to avoid requiring a CraftBukkit runtime.
 * Player.getHealth() returns 20.0 by default (Mockito stub).
 *
 * Region membership is now resolved via RegionPresenceTask cache, so tests
 * stub getCachedRegion(uuid) instead of regionManager.getRegionAt(location).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DamageCapListenerTest {

    @Mock ExtractionEventPlugin plugin;
    @Mock RegionPresenceTask    presenceTask;
    @Mock FileConfiguration     config;
    @Mock Player                victim;
    @Mock LevRegion             region;

    private UUID victimId;
    DamageCapListener listener;

    @BeforeEach
    void setUp() {
        victimId = UUID.randomUUID();
        when(victim.getUniqueId()).thenReturn(victimId);
        when(plugin.getRegionPresenceTask()).thenReturn(presenceTask);
        when(plugin.getConfig()).thenReturn(config);
        when(config.getDouble("warzone.max-single-hit", 18.0)).thenReturn(18.0);
        when(config.getDouble("warzone.min-survival-hp", 0.5)).thenReturn(0.5);
        // Default region flag: capped
        when(region.isDamageCapped()).thenReturn(true);
        listener = new DamageCapListener(plugin);
    }

    // ── Bug 5: survival floor ─────────────────────────────────────────────────

    @Test
    void hitThatWouldKill_shouldBeFlooredToSurvivalHp() {
        when(presenceTask.getCachedRegion(victimId)).thenReturn(region);
        when(victim.getHealth()).thenReturn(2.0); // 1 heart

        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getFinalDamage()).thenReturn(5.0); // 5 > 2 → would kill
        when(event.getDamage()).thenReturn(5.0);

        listener.onEntityDamage(event);

        verify(event).setDamage(anyDouble());
        verify(event, never()).setCancelled(true);
    }

    @Test
    void hitThatDoesNotKill_shouldNotBeModified() {
        when(presenceTask.getCachedRegion(victimId)).thenReturn(region);
        when(victim.getHealth()).thenReturn(20.0); // full HP

        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getFinalDamage()).thenReturn(5.0); // safe hit

        listener.onEntityDamage(event);

        verify(event, never()).setDamage(anyDouble());
        verify(event, never()).setCancelled(true);
    }

    @Test
    void hitOutsideRegion_shouldNotApplySurvivalFloor() {
        when(presenceTask.getCachedRegion(victimId)).thenReturn(null);

        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(victim);

        listener.onEntityDamage(event);

        verify(event, never()).setDamage(anyDouble());
    }

    // ── Bug 6: aura/enchant damage cap ────────────────────────────────────────

    @Test
    void burstDamageExceedingCap_shouldBeCapped() {
        when(presenceTask.getCachedRegion(victimId)).thenReturn(region);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getFinalDamage()).thenReturn(25.0); // exceeds 18 cap

        listener.onEntityDamageByEntity(event);

        verify(event).setDamage(18.0);
    }

    @Test
    void damageWithinCap_shouldNotBeModified() {
        when(presenceTask.getCachedRegion(victimId)).thenReturn(region);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getFinalDamage()).thenReturn(10.0); // within cap

        listener.onEntityDamageByEntity(event);

        verify(event, never()).setDamage(anyDouble());
    }

    @Test
    void burstDamageOutsideRegion_shouldNotBeCapped() {
        when(presenceTask.getCachedRegion(victimId)).thenReturn(null);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);

        listener.onEntityDamageByEntity(event);

        verify(event, never()).setDamage(anyDouble());
    }

    @Test
    void hitInRegionWithCapDisabled_shouldNotApplyCap() {
        when(region.isDamageCapped()).thenReturn(false);
        when(presenceTask.getCachedRegion(victimId)).thenReturn(region);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getFinalDamage()).thenReturn(25.0); // would exceed cap, but flag off

        listener.onEntityDamageByEntity(event);

        verify(event, never()).setDamage(anyDouble());
    }
}
