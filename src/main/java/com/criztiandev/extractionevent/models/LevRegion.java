package com.criztiandev.extractionevent.models;

public class LevRegion {
    private String id;
    private final String world;
    private final int minX, maxX, minZ, maxZ;
    private int minY = -64;
    private int maxY = 320;

    // ── Core toggles (original) ─────────────────────────────────────────────
    private boolean blockEnderPearl      = true;
    private boolean hideNameTags         = true;
    private boolean lightningOnDeath     = true;
    private boolean spawnMimic           = false;

    // ── Warzone protection toggles ──────────────────────────────────────────
    private boolean freeCamBlocked       = true;   // FreeCamListener
    private boolean damageCapped         = true;   // DamageCapListener
    private boolean enderChestRestricted = true;   // EnderChestListener
    private boolean killEffectEnabled    = true;   // KillEffectListener lightning

    public LevRegion(String id, String world, int minX, int maxX, int minZ, int maxZ) {
        this.id    = id;
        this.world = world;
        this.minX  = minX;
        this.maxX  = maxX;
        this.minZ  = minZ;
        this.maxZ  = maxZ;
    }

    public String getId()             { return id; }
    public void   setId(String id)   { this.id = id; }
    public String getWorld()          { return world; }

    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public int getMinY() { return minY; }
    public void setMinY(int minY)     { this.minY = minY; }
    public int getMaxY()              { return maxY; }
    public void setMaxY(int maxY)     { this.maxY = maxY; }

    // ── Getters / setters for all toggles ───────────────────────────────────

    public boolean isBlockEnderPearl()            { return blockEnderPearl; }
    public void    setBlockEnderPearl(boolean v)  { blockEnderPearl = v; }

    public boolean isHideNameTags()               { return hideNameTags; }
    public void    setHideNameTags(boolean v)     { hideNameTags = v; }

    public boolean isLightningOnDeath()           { return lightningOnDeath; }
    public void    setLightningOnDeath(boolean v) { lightningOnDeath = v; }

    public boolean isSpawnMimic()                 { return spawnMimic; }
    public void    setSpawnMimic(boolean v)       { spawnMimic = v; }

    public boolean isFreeCamBlocked()             { return freeCamBlocked; }
    public void    setFreeCamBlocked(boolean v)   { freeCamBlocked = v; }

    public boolean isDamageCapped()               { return damageCapped; }
    public void    setDamageCapped(boolean v)     { damageCapped = v; }

    public boolean isEnderChestRestricted()           { return enderChestRestricted; }
    public void    setEnderChestRestricted(boolean v) { enderChestRestricted = v; }

    public boolean isKillEffectEnabled()          { return killEffectEnabled; }
    public void    setKillEffectEnabled(boolean v){ killEffectEnabled = v; }
}
