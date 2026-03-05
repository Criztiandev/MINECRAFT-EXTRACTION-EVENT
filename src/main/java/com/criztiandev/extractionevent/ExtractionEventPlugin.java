package com.criztiandev.extractionevent;

import com.criztiandev.extractionevent.commands.LevCommand;
import com.criztiandev.extractionevent.listeners.RegionWandListener;
import com.criztiandev.extractionevent.managers.RegionManager;
import com.criztiandev.extractionevent.storage.JsonStorageProvider;
import com.criztiandev.extractionevent.storage.StorageProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class ExtractionEventPlugin extends JavaPlugin {

    private StorageProvider storageProvider;
    private RegionManager regionManager;

    private com.criztiandev.extractionevent.managers.NameTagManager nameTagManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize Managers
        storageProvider = new com.criztiandev.extractionevent.storage.JsonStorageProvider(this);
        regionManager = new RegionManager(this);
        nameTagManager = new com.criztiandev.extractionevent.managers.NameTagManager(this);

        // Load Regions
        regionManager.loadAll();

        // Register Listeners
        getServer().getPluginManager().registerEvents(new RegionWandListener(this), this);
        getServer().getPluginManager().registerEvents(new com.criztiandev.extractionevent.listeners.LevEventListener(this), this);
        getServer().getPluginManager().registerEvents(new com.criztiandev.extractionevent.gui.GuiManager(this), this);

        // Register Commands
        getCommand("lev").setExecutor(new LevCommand(this));

        // Start Tasks
        new com.criztiandev.extractionevent.tasks.RegionPresenceTask(this).runTaskTimer(this, 10L, 10L); // Run every 0.5 seconds

        getLogger().info("ExtractionEvent has been enabled!");
    }

    @Override
    public void onDisable() {
        if (nameTagManager != null) {
            nameTagManager.cleanup();
        }
        getLogger().info("ExtractionEvent has been disabled!");
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }
    
    public com.criztiandev.extractionevent.managers.NameTagManager getNameTagManager() {
        return nameTagManager;
    }
}
