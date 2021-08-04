package io.harness.eventsframework.impl.redis;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import org.redisson.api.RListMultimapCache;
import org.redisson.api.RMapCache;

@OwnedBy(HarnessTeam.PIPELINE)
public interface DistributedCache {
  <K, V> RMapCache<K, V> getMap(String key);

  <K, V> void putInsideMap(String key, K innerKey, V value);

  <K, V> RListMultimapCache<K, V> getMultiMap(String key);

  <K, V> void putInsideMultiMap(String key, K innerKey, V value);

  <K, V> V getFromMultiMap(String key, K innerKey);

  <K, V> boolean presentInMultiMap(String key, K innerKey, V value);

  <K, V> boolean presentInMap(String key, K innerKey);

  <K, V> V getFromMap(String key, K innerKey);
}
