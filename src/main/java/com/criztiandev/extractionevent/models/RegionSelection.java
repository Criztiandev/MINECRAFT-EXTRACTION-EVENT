package com.criztiandev.extractionevent.models;

import org.bukkit.Location;

public class RegionSelection {
    private Location pos1;
    private Location pos2;

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null && pos1.getWorld().equals(pos2.getWorld());
    }

    public int getMinX() {
        return Math.min(pos1.getBlockX(), pos2.getBlockX());
    }

    public int getMaxX() {
        return Math.max(pos1.getBlockX(), pos2.getBlockX());
    }

    public int getMinZ() {
        return Math.min(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public int getMaxZ() {
        return Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public int getMinY() {
        return Math.min(pos1.getBlockY(), pos2.getBlockY());
    }

    public int getMaxY() {
        return Math.max(pos1.getBlockY(), pos2.getBlockY());
    }

    public String getWorldName() {
        return pos1.getWorld().getName();
    }
}
