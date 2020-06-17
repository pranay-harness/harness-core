package io.harness.cache;

import static io.harness.cache.CacheBackend.CAFFEINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import java.util.Optional;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.ExpiryPolicy;

public class HarnessCacheManagerImpl implements HarnessCacheManager {
  private CacheManager cacheManager;
  private CacheConfig cacheConfig;

  public HarnessCacheManagerImpl(CacheManager cacheManager, CacheConfig cacheConfig) {
    this.cacheManager = cacheManager;
    this.cacheConfig = cacheConfig;
  }

  @Override
  public <K, V> Cache<K, V> getCache(
      String cacheName, Class<K> keyType, Class<V> valueType, Factory<ExpiryPolicy> expiryPolicy) {
    if (isNotEmpty(cacheConfig.getDisabledCaches()) && cacheConfig.getDisabledCaches().contains(cacheName)) {
      return new NoOpCache<>();
    }

    MutableConfiguration<K, V> jCacheConfiguration = new MutableConfiguration<>();
    jCacheConfiguration.setTypes(keyType, valueType);
    jCacheConfiguration.setStoreByValue(cacheConfig.getCacheBackend() != CAFFEINE);
    jCacheConfiguration.setExpiryPolicyFactory(expiryPolicy);
    jCacheConfiguration.setStatisticsEnabled(true);
    jCacheConfiguration.setManagementEnabled(true);

    try {
      return Optional.ofNullable(cacheManager.getCache(cacheName, keyType, valueType))
          .orElseGet(() -> cacheManager.createCache(cacheName, jCacheConfiguration));
    } catch (CacheException ce) {
      if (isCacheExistsError(ce, cacheName)) {
        return cacheManager.getCache(cacheName, keyType, valueType);
      }
      throw ce;
    }
  }

  private boolean isCacheExistsError(CacheException ce, String cacheName) {
    return ce.getMessage().equalsIgnoreCase("Cache " + cacheName + " already exists")
        || ce.getMessage().equalsIgnoreCase("A cache named " + cacheName + " already exists.");
  }
}
