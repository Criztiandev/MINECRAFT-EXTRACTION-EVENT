package com.criztiandev.extractionevent.managers;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.storage.StateStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Level;

/**
 * Bridges ExtractionEvent commands to ExtractionRegionEditor's SavedRegion lockdown API.
 *
 * Two modes:
 *   LOCKDOWN — all EXTRACTION-type regions are locked; nobody can extract.
 *   KOTH     — all regions are locked EXCEPT one designated "king" region.
 *              The king region is the only point where extraction succeeds.
 *
 * Uses reflection so ExtractionEvent does NOT compile-time depend on ExtractionRegionEditor classes.
 */
public class LockdownManager {

    private enum Mode { NONE, LOCKDOWN, KOTH }

    private final ExtractionEventPlugin plugin;

    private Mode activeMode  = Mode.NONE;
    private String kothRegionId = null;

    // Cached reflection references (initialised lazily once on first use)
    private Object      regionManager    = null;
    private Method      getRegionsMethod = null;
    private Method      setLockedMethod  = null;

    public LockdownManager(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean isLockdownActive()  { return activeMode == Mode.LOCKDOWN; }
    public boolean isKothActive()      { return activeMode == Mode.KOTH; }
    public String  getKothRegionId()   { return kothRegionId; }

    /**
     * Activates full lockdown — blocks all extraction points immediately.
     * Cancels any in-progress extraction sessions.
     */
    public boolean startLockdown(Player initiator) {
        if (!initReflection()) return false;

        setAllLocked(true, null);
        cancelAllSessions();

        activeMode   = Mode.LOCKDOWN;
        kothRegionId = null;

        String msg = "§8[§4⚠ LOCKDOWN§8] §cAll extraction points have been locked by §e" + initiator.getName() + "§c!";
        Bukkit.broadcastMessage(msg);
        plugin.getLogger().info("[Lockdown] Started by " + initiator.getName());
        return true;
    }

    /**
     * Activates KOTH mode — locks every extraction region EXCEPT {@code kothId}.
     * Players fight over the single open point.
     */
    public boolean startKoth(Player initiator, String kothId) {
        if (!initReflection()) return false;

        // Verify the target region exists
        if (!regionExists(kothId)) return false;

        setAllLocked(true, kothId);   // lock everything
        setRegionLocked(kothId, false); // then unlock the king
        cancelAllSessionsExcept(kothId);

        activeMode   = Mode.KOTH;
        kothRegionId = kothId;

        String msg = "§8[§6⚔ KOTH§8] §eKing of the Hill active! §7Only §a" + kothId + " §7is open for extraction!";
        Bukkit.broadcastMessage(msg);
        plugin.getLogger().info("[KOTH] Started by " + initiator.getName() + " — king region: " + kothId);
        return true;
    }

    /**
     * Deactivates lockdown/KOTH — unlocks all extraction regions.
     */
    public boolean stop(Player initiator) {
        if (activeMode == Mode.NONE) return false;
        if (!initReflection()) return false;

        setAllLocked(false, null);

        Mode wasMode = activeMode;
        activeMode   = Mode.NONE;
        kothRegionId = null;

        String label = wasMode == Mode.KOTH ? "§6KOTH" : "§4LOCKDOWN";
        Bukkit.broadcastMessage("§8[" + label + "§8] §aAll extraction points are now §aopen§a. Initiated by §e" + initiator.getName() + "§a.");
        plugin.getLogger().info("[" + wasMode + "] Stopped by " + initiator.getName());
        return true;
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

    private boolean initReflection() {
        if (regionManager != null) return true;
        try {
            Plugin erPlugin = Bukkit.getPluginManager().getPlugin("ExtractionRegionEditor");
            if (erPlugin == null) {
                plugin.getLogger().warning("[LockdownManager] ExtractionRegionEditor not found.");
                return false;
            }
            regionManager    = erPlugin.getClass().getMethod("getRegionManager").invoke(erPlugin);
            getRegionsMethod = regionManager.getClass().getMethod("getRegions");

            // Find setLockedDown in SavedRegion — located via the first region
            Collection<?> sample = (Collection<?>) getRegionsMethod.invoke(regionManager);
            if (!sample.isEmpty()) {
                setLockedMethod = sample.iterator().next().getClass().getMethod("setLockedDown", boolean.class);
            }
            return regionManager != null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[LockdownManager] Reflection init failed", e);
            regionManager = null;
            return false;
        }
    }

    private void setAllLocked(boolean locked, String exemptId) {
        try {
            Collection<?> regions = (Collection<?>) getRegionsMethod.invoke(regionManager);
            for (Object region : regions) {
                String id = (String) region.getClass().getMethod("getId").invoke(region);
                if (exemptId != null && exemptId.equalsIgnoreCase(id)) continue;
                getOrInitSetLocked(region).invoke(region, locked);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[LockdownManager] setAllLocked failed", e);
        }
    }

    private void setRegionLocked(String regionId, boolean locked) {
        try {
            Collection<?> regions = (Collection<?>) getRegionsMethod.invoke(regionManager);
            for (Object region : regions) {
                String id = (String) region.getClass().getMethod("getId").invoke(region);
                if (regionId.equalsIgnoreCase(id)) {
                    getOrInitSetLocked(region).invoke(region, locked);
                    return;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[LockdownManager] setRegionLocked failed", e);
        }
    }

    private boolean regionExists(String regionId) {
        try {
            Collection<?> regions = (Collection<?>) getRegionsMethod.invoke(regionManager);
            for (Object region : regions) {
                String id = (String) region.getClass().getMethod("getId").invoke(region);
                if (regionId.equalsIgnoreCase(id)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private Method getOrInitSetLocked(Object region) throws NoSuchMethodException {
        if (setLockedMethod != null) return setLockedMethod;
        setLockedMethod = region.getClass().getMethod("setLockedDown", boolean.class);
        return setLockedMethod;
    }

    /** Force-cancel all active extraction sessions. */
    private void cancelAllSessions() {
        cancelAllSessionsExcept(null);
    }

    /** Cancel sessions for all regions except optionally exemptId. */
    private void cancelAllSessionsExcept(String exemptId) {
        try {
            Plugin erPlugin = Bukkit.getPluginManager().getPlugin("ExtractionRegionEditor");
            if (erPlugin == null) return;
            Object extractionTask = erPlugin.getClass().getMethod("getExtractionTask").invoke(erPlugin);
            java.util.Map<?, ?> sessions = (java.util.Map<?, ?>) extractionTask.getClass().getMethod("getSessions").invoke(extractionTask);

            sessions.entrySet().removeIf(entry -> {
                try {
                    Object session = entry.getValue();
                    Object region  = session.getClass().getMethod("getRegion").invoke(session);
                    String id      = (String) region.getClass().getMethod("getId").invoke(region);
                    if (exemptId != null && exemptId.equalsIgnoreCase(id)) return false;

                    // Notify the player their session was cancelled
                    java.util.UUID uuid = (java.util.UUID) entry.getKey();
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.sendMessage("§c✘ Your extraction was cancelled by an admin action.");
                    }
                    return true;
                } catch (Exception ignored) {
                    return false;
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[LockdownManager] cancelAllSessions failed", e);
        }
    }

    /** Returns all EXTRACTION region IDs from ExtractionRegionEditor. */
    public java.util.List<String> getExtractionRegionIds() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        if (!initReflection()) return ids;
        try {
            Collection<?> regions = (Collection<?>) getRegionsMethod.invoke(regionManager);
            for (Object region : regions) {
                // Only include EXTRACTION type regions
                Object type = region.getClass().getMethod("getType").invoke(region);
                if (!"EXTRACTION".equals(type.toString())) continue;
                ids.add((String) region.getClass().getMethod("getId").invoke(region));
            }
        } catch (Exception ignored) {}
        return ids;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /** Called by ExtractionEventPlugin.shutdown() — writes mode and kothId to state. */
    public void captureState(StateStore.State state) {
        if (activeMode == Mode.LOCKDOWN) {
            state.lockdownMode = StateStore.LockdownMode.LOCKDOWN;
        } else if (activeMode == Mode.KOTH) {
            state.lockdownMode = StateStore.LockdownMode.KOTH;
            state.kothRegionId = kothRegionId;
        } else {
            state.lockdownMode = StateStore.LockdownMode.NONE;
        }
    }

    /**
     * Called one tick after onEnable() — restores the lockdown state
     * silently (no broadcast) since players already knew about it before the reload.
     */
    public void restoreState(StateStore.State state) {
        if (state.lockdownMode == StateStore.LockdownMode.LOCKDOWN) {
            if (!initReflection()) return;
            setAllLocked(true, null);
            activeMode   = Mode.LOCKDOWN;
            kothRegionId = null;
            plugin.getLogger().info("[LockdownManager] Lockdown restored from state.");
        } else if (state.lockdownMode == StateStore.LockdownMode.KOTH && state.kothRegionId != null) {
            if (!initReflection()) return;
            setAllLocked(true, state.kothRegionId);
            setRegionLocked(state.kothRegionId, false);
            activeMode   = Mode.KOTH;
            kothRegionId = state.kothRegionId;
            plugin.getLogger().info("[LockdownManager] KOTH restored — king: " + kothRegionId);
        }
    }
}
