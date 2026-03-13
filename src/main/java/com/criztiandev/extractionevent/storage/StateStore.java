package com.criztiandev.extractionevent.storage;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Persists volatile runtime state that must survive plugin reloads and server restarts:
 *   - Lockdown mode (NONE / LOCKDOWN / KOTH) and the active KOTH region id
 *   - Active Warzone Shift expiry timestamps (regionId → epochMs)
 *
 * Written to plugins/extraction-event-handler/state.json.
 * Uses atomic write (temp → rename) so a crash mid-save never corrupts the file.
 */
public class StateStore {

    public enum LockdownMode { NONE, LOCKDOWN, KOTH }

    public static class State {
        @SerializedName("lockdownMode") public LockdownMode lockdownMode = LockdownMode.NONE;
        @SerializedName("kothRegionId") public String       kothRegionId = null;
        @SerializedName("activeShifts") public Map<String, Long> activeShifts = new HashMap<>();
    }

    private final ExtractionEventPlugin plugin;
    private final File stateFile;
    private final File tempFile;
    private final Gson gson;

    public StateStore(ExtractionEventPlugin plugin) {
        this.plugin    = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "state.json");
        this.tempFile  = new File(plugin.getDataFolder(), "state.json.tmp");
        this.gson      = new GsonBuilder().setPrettyPrinting().create();
    }

    public State load() {
        if (!stateFile.exists()) return new State();
        try (FileReader reader = new FileReader(stateFile)) {
            State s = gson.fromJson(reader, State.class);
            if (s == null) return new State();
            if (s.activeShifts == null) s.activeShifts = new HashMap<>();
            // Discard expired shifts so we don't resurrect dead events
            long now = System.currentTimeMillis();
            s.activeShifts.values().removeIf(expiry -> expiry <= now);
            return s;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[StateStore] Failed to load state.json — starting fresh.", e);
            return new State();
        }
    }

    public void save(State state) {
        try {
            plugin.getDataFolder().mkdirs();
            try (FileWriter writer = new FileWriter(tempFile)) {
                gson.toJson(state, writer);
            }
            // Atomic rename: never leaves the permanent file in a half-written state
            if (stateFile.exists()) stateFile.delete();
            if (!tempFile.renameTo(stateFile)) {
                plugin.getLogger().warning("[StateStore] Atomic rename failed — state may not be saved.");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[StateStore] Failed to save state.json.", e);
        }
    }
}
