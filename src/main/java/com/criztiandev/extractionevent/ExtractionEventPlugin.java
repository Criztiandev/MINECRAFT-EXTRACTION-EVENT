package com.criztiandev.extractionevent;

import com.criztiandev.extractionevent.commands.LevCommand;
import com.criztiandev.extractionevent.gui.GuiManager;
import com.criztiandev.extractionevent.listeners.DamageCapListener;
import com.criztiandev.extractionevent.listeners.EnderChestListener;
import com.criztiandev.extractionevent.listeners.FreeCamListener;
import com.criztiandev.extractionevent.listeners.KillEffectListener;
import com.criztiandev.extractionevent.listeners.LevEventListener;
import com.criztiandev.extractionevent.listeners.RegionWandListener;
import com.criztiandev.extractionevent.managers.MimicManager;
import com.criztiandev.extractionevent.managers.MinimapHideManager;
import com.criztiandev.extractionevent.managers.NameTagManager;
import com.criztiandev.extractionevent.managers.RegionManager;
import com.criztiandev.extractionevent.storage.JsonStorageProvider;
import com.criztiandev.extractionevent.storage.StorageProvider;
import com.criztiandev.extractionevent.tasks.MimicSpawnTask;
import com.criztiandev.extractionevent.tasks.RegionPresenceTask;
import com.criztiandev.extractionevent.tasks.WarzoneAfkTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ExtractionEventPlugin extends JavaPlugin {

    private StorageProvider    storageProvider;
    private RegionManager      regionManager;
    private NameTagManager     nameTagManager;
    private MimicManager       mimicManager;
    private MinimapHideManager minimapHideManager;
    private RegionPresenceTask regionPresenceTask;
    private MimicSpawnTask     mimicSpawnTask;
    private WarzoneAfkTask     warzoneAfkTask;

    /** Admins who opted into test mode — they are treated as regular players for restriction checks. */
    private final Set<UUID> testModeAdmins = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        storageProvider = new JsonStorageProvider(this);
        regionManager   = new RegionManager(this);
        nameTagManager  = new NameTagManager(this);
        mimicManager    = new MimicManager(this);

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            minimapHideManager = new MinimapHideManager(this);
            getLogger().info("ProtocolLib detected — minimap player hiding enabled.");
        } else {
            getLogger().warning("ProtocolLib not found — minimap hiding uses name-only mode.");
        }

        regionManager.loadAll();

        // ── Event listeners ───────────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new RegionWandListener(this), this);
        getServer().getPluginManager().registerEvents(new LevEventListener(this),  this);
        getServer().getPluginManager().registerEvents(new GuiManager(this),        this);
        getServer().getPluginManager().registerEvents(new EnderChestListener(this),this);
        getServer().getPluginManager().registerEvents(new FreeCamListener(this),   this);
        getServer().getPluginManager().registerEvents(new DamageCapListener(this), this);
        getServer().getPluginManager().registerEvents(new KillEffectListener(this),this);

        // ── Commands ──────────────────────────────────────────────────────────
        LevCommand levCommand = new LevCommand(this);
        var commandObj = getCommand("lrev");
        if (commandObj != null) {
            commandObj.setExecutor(levCommand);
            commandObj.setTabCompleter(levCommand); // same class handles tab-complete
        }

        // ── Scheduled tasks ───────────────────────────────────────────────────
        regionPresenceTask = new RegionPresenceTask(this);
        regionPresenceTask.runTaskTimer(this, 20L, 20L); // 1 s
        mimicSpawnTask = new MimicSpawnTask(this);
        mimicSpawnTask.runTaskTimer(this, 100L, 100L); // 5 s
        warzoneAfkTask = new WarzoneAfkTask(this);
        getServer().getPluginManager().registerEvents(warzoneAfkTask, this);
        warzoneAfkTask.runTaskTimer(this, 40L, 20L); // check every 1 s, first run after 2 s

        getLogger().info("ExtractionEvent enabled!");
    }

    @Override
    public void onDisable() {
        shutdown();
        getLogger().info("ExtractionEvent disabled.");
    }

    /**
     * Clean reload — safe to call from /lev reload AND from Plugman reload.
     *
     * Plugman calls onDisable() then onEnable() on the SAME class instance,
     * so we need teardown and init to be idempotent.
     */
    public void reload(org.bukkit.command.CommandSender sender) {
        shutdown();

        reloadConfig(); // re-reads config.yml from disk
        regionManager.loadAll();

        // Recreate managers that cache config values
        nameTagManager = new NameTagManager(this);

        // Restart scheduled tasks
        regionPresenceTask = new RegionPresenceTask(this);
        regionPresenceTask.runTaskTimer(this, 20L, 20L);
        mimicSpawnTask = new MimicSpawnTask(this);
        mimicSpawnTask.runTaskTimer(this, 100L, 100L);
        warzoneAfkTask = new WarzoneAfkTask(this);
        getServer().getPluginManager().registerEvents(warzoneAfkTask, this);
        warzoneAfkTask.runTaskTimer(this, 40L, 20L);

        sender.sendMessage("§a[ExtractionEvent] Reloaded successfully.");
        getLogger().info("ExtractionEvent reloaded by " + sender.getName());
    }

    /** Cancels tasks and restores all player states — called on disable and before reload. */
    private void shutdown() {
        if (regionPresenceTask != null && !regionPresenceTask.isCancelled()) {
            regionPresenceTask.cancel();
        }
        if (mimicSpawnTask != null && !mimicSpawnTask.isCancelled()) {
            mimicSpawnTask.cancel();
        }
        if (warzoneAfkTask != null && !warzoneAfkTask.isCancelled()) {
            warzoneAfkTask.cancel();
        }
        if (nameTagManager != null) {
            nameTagManager.cleanup();
        }
    }

    public StorageProvider    getStorageProvider()   { return storageProvider; }
    public RegionManager      getRegionManager()     { return regionManager; }
    public NameTagManager     getNameTagManager()    { return nameTagManager; }
    public MimicManager       getMimicManager()      { return mimicManager; }
    public MinimapHideManager getMinimapHideManager(){ return minimapHideManager; }
    public RegionPresenceTask getRegionPresenceTask(){ return regionPresenceTask; }

    /** @return true if this admin has test mode ON (treats them as a normal player). */
    public boolean isTestMode(UUID uuid) { return testModeAdmins.contains(uuid); }

    /** Toggles test mode for this admin. @return true if now ON. */
    public boolean toggleTestMode(UUID uuid) {
        if (testModeAdmins.remove(uuid)) return false;
        testModeAdmins.add(uuid);
        return true;
    }
}
