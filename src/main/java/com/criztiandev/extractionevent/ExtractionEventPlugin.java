package com.criztiandev.extractionevent;

import org.bukkit.plugin.java.JavaPlugin;

public class ExtractionEventPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    getLogger().info("ExtractionEvent has been enabled!");
    saveDefaultConfig();
  }

  @Override
  public void onDisable() {
    getLogger().info("ExtractionEvent has been disabled!");
  }
}
