package io.harness.cache;

import java.time.Duration;
import java.util.List;

public interface DistributedStore {
  // Returns the stored cache - whatever changeHash
  <T extends Distributable> T get(long algorithmId, long structureHash, String key, List<String> params);

  // Returns the stored cache - only with exact changeHash
  <T extends Distributable> T get(
      long contextHash, long algorithmId, long structureHash, String key, List<String> params);

  // Inserts or updates the cache for entity
  <T extends Distributable> void upsert(T entity, Duration ttl);
}
