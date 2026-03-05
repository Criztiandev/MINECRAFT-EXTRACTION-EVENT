package com.criztiandev.extractionevent.storage;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JsonStorageProvider implements StorageProvider {

    private final ExtractionEventPlugin plugin;
    private final File folder;
    private final Gson gson;

    public JsonStorageProvider(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "regions");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public CompletableFuture<List<LevRegion>> loadAllRegions() {
        return CompletableFuture.supplyAsync(() -> {
            List<LevRegion> regions = new ArrayList<>();
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));

            if (files != null) {
                for (File file : files) {
                    try (FileReader reader = new FileReader(file)) {
                        LevRegion region = gson.fromJson(reader, LevRegion.class);
                        if (region != null) {
                            regions.add(region);
                        }
                    } catch (IOException e) {
                        plugin.getLogger().severe("Could not read region file " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
            return regions;
        });
    }

    @Override
    public CompletableFuture<Boolean> saveRegion(LevRegion region) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(folder, region.getId().toLowerCase() + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(region, writer);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save region " + region.getId() + ": " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteRegion(String id) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(folder, id.toLowerCase() + ".json");
            if (file.exists()) {
                return file.delete();
            }
            return false;
        });
    }
}
