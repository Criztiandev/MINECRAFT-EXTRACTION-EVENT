package com.criztiandev.extractionevent.models;

public class LevRegion {
    private String id;
    private final String world;
    private final int minX, maxX, minZ, maxZ;
    private int minY = -64;
    private int maxY = 320;
    
    private boolean blockEnderPearl = true;
    private boolean hideNameTags = true;
    private boolean lightningOnDeath = true;

    public LevRegion(String id, String world, int minX, int maxX, int minZ, int maxZ) {
        this.id = id;
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorld() {
        return world;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getMinY() {
        return minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public boolean isBlockEnderPearl() {
        return blockEnderPearl;
    }

    public void setBlockEnderPearl(boolean blockEnderPearl) {
        this.blockEnderPearl = blockEnderPearl;
    }

    public boolean isHideNameTags() {
        return hideNameTags;
    }

    public void setHideNameTags(boolean hideNameTags) {
        this.hideNameTags = hideNameTags;
    }

    public boolean isLightningOnDeath() {
        return lightningOnDeath;
    }

    public void setLightningOnDeath(boolean lightningOnDeath) {
        this.lightningOnDeath = lightningOnDeath;
    }
}
