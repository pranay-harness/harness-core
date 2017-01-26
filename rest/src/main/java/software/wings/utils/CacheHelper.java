package software.wings.utils;

import java.util.Optional;
import javax.cache.Cache;
import javax.cache.Caching;

/**
 * Created by peeyushaggarwal on 1/26/17.
 */
public class CacheHelper {
  public static <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
    return Optional.ofNullable(Caching.getCache(cacheName, keyType, valueType))
        .orElseGet(()
                       -> Caching.getCachingProvider().getCacheManager().createCache(
                           cacheName, new javax.cache.configuration.Configuration<K, V>() {
                             private static final long serialVersionUID = 1L;

                             @Override
                             public Class<K> getKeyType() {
                               return keyType;
                             }

                             @Override
                             public Class<V> getValueType() {
                               return valueType;
                             }

                             @Override
                             public boolean isStoreByValue() {
                               return true;
                             }
                           }));
  }
}
