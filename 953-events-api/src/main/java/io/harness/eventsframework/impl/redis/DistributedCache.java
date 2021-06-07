package io.harness.eventsframework.impl.redis;

import org.redisson.api.RListMultimapCache;
import org.redisson.api.RMapCache;

public interface DistributedCache {
  <K, V> RMapCache<K, V> getMap(String key);

  <K, V> void putInsideMap(String key, K innerKey, V value);

  <K, V> RListMultimapCache<K, V> getMultiMap(String key);

  <K, V> void putInsideMultiMap(String key, K innerKey, V value);
}
