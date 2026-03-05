package com.criztiandev.extractionevent.storage;

import com.criztiandev.extractionevent.models.LevRegion;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {

    CompletableFuture<List<LevRegion>> loadAllRegions();

    CompletableFuture<Boolean> saveRegion(LevRegion region);

    CompletableFuture<Boolean> deleteRegion(String id);

}
