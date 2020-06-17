package io.harness.cache;

import static io.harness.cache.CacheBackend.CAFFEINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
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
  static final String CACHE_PREFIX = "hCache";

  HarnessCacheManagerImpl(CacheManager cacheManager, CacheConfig cacheConfig) {
    this.cacheManager = cacheManager;
    this.cacheConfig = cacheConfig;
  }

  @Override
  public <K, V> Cache<K, V> getCache(
      String cacheName, Class<K> keyType, Class<V> valueType, Factory<ExpiryPolicy> expiryPolicy) {
    if (isNotEmpty(cacheConfig.getDisabledCaches()) && cacheConfig.getDisabledCaches().contains(cacheName)) {
      return new NoOpCache<>();
    }
    String cacheNamespace = isEmpty(cacheConfig.getCacheNamespace())
        ? CACHE_PREFIX
        : cacheConfig.getCacheNamespace().concat("/").concat(CACHE_PREFIX);
    String internalCacheName = String.format("%s/%s", cacheNamespace, cacheName);
    MutableConfiguration<K, V> jCacheConfiguration = new MutableConfiguration<>();
    jCacheConfiguration.setTypes(keyType, valueType);
    jCacheConfiguration.setStoreByValue(cacheConfig.getCacheBackend() != CAFFEINE);
    jCacheConfiguration.setExpiryPolicyFactory(expiryPolicy);
    jCacheConfiguration.setStatisticsEnabled(true);
    jCacheConfiguration.setManagementEnabled(true);

    try {
      return Optional.ofNullable(cacheManager.getCache(internalCacheName, keyType, valueType))
          .orElseGet(() -> cacheManager.createCache(internalCacheName, jCacheConfiguration));
    } catch (CacheException ce) {
      if (isCacheExistsError(ce, internalCacheName)) {
        return cacheManager.getCache(internalCacheName, keyType, valueType);
      }
      throw ce;
    }
  }

  private boolean isCacheExistsError(CacheException ce, String cacheName) {
    return ce.getMessage().equalsIgnoreCase("Cache " + cacheName + " already exists")
        || ce.getMessage().equalsIgnoreCase("A cache named " + cacheName + " already exists.");
  }
}
