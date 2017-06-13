package software.wings.utils;

import software.wings.beans.User;

import java.util.Optional;
import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;
import javax.inject.Singleton;

/**
 * Created by peeyushaggarwal on 1/26/17.
 */
@Singleton
public class CacheHelper {
  public <K, V> Cache<K, V> getCache(
      String cacheName, Class<K> keyType, Class<V> valueType, Factory<ExpiryPolicy> expiryPolicy) {
    MutableConfiguration<K, V> configuration = new MutableConfiguration<>();
    configuration.setTypes(keyType, valueType);
    configuration.setStoreByValue(true);
    configuration.setExpiryPolicyFactory(expiryPolicy);
    return Optional.ofNullable(Caching.getCache(cacheName, keyType, valueType))
        .orElseGet(() -> Caching.getCachingProvider().getCacheManager().createCache(cacheName, configuration));
  }

  public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
    return getCache(cacheName, keyType, valueType, EternalExpiryPolicy.factoryOf());
  }

  public Cache<String, User> getUserCache() {
    return getCache("userCache", String.class, User.class, AccessedExpiryPolicy.factoryOf(Duration.THIRTY_MINUTES));
  }
}
