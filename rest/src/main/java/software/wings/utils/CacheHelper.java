package software.wings.utils;

import java.util.Optional;
import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.ExpiryPolicy;

/**
 * Created by peeyushaggarwal on 1/26/17.
 */
public class CacheHelper {
  public static <K, V> Cache<K, V> getCache(
      String cacheName, Class<K> keyType, Class<V> valueType, Factory<ExpiryPolicy> expiryPolicy) {
    MutableConfiguration<K, V> configuration = new MutableConfiguration<>();
    configuration.setStoreByValue(true);
    configuration.setExpiryPolicyFactory(expiryPolicy);
    return Optional.ofNullable(Caching.getCache(cacheName, keyType, valueType))
        .orElseGet(() -> Caching.getCachingProvider().getCacheManager().createCache(cacheName, configuration));
  }

  public static <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
    MutableConfiguration<K, V> configuration = new MutableConfiguration<>();
    configuration.setStoreByValue(true);
    return Optional.ofNullable(Caching.getCache(cacheName, keyType, valueType))
        .orElseGet(() -> Caching.getCachingProvider().getCacheManager().createCache(cacheName, configuration));
  }
}
