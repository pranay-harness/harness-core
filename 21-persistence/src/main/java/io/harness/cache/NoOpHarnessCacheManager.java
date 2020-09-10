package io.harness.cache;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.cache.Cache;
import javax.cache.configuration.Factory;
import javax.cache.expiry.ExpiryPolicy;

@OwnedBy(PL)
public class NoOpHarnessCacheManager implements HarnessCacheManager {
  @Override
  public <K, V> Cache<K, V> getCache(
      String cacheName, Class<K> keyType, Class<V> valueType, Factory<ExpiryPolicy> expiryPolicy) {
    return new NoOpCache<>();
  }

  @Override
  public <K, V> Cache<K, V> getCache(
      String cacheName, Class<K> keyType, Class<V> valueType, Factory<ExpiryPolicy> expiryPolicy, String keyPrefix) {
    return new NoOpCache<>();
  }
}
