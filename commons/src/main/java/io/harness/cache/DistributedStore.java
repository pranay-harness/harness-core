package io.harness.cache;

import java.time.Duration;

public interface DistributedStore {
  <T extends Distributable> T get(long contextHash, long algorithmId, long structureHash, String key);
  <T extends Distributable> void upsert(T obj, Duration ttl);
}
