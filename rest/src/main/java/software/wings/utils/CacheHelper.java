package software.wings.utils;

import static software.wings.common.Constants.NEW_RELIC_APPLICATION_CACHE;
import static software.wings.common.Constants.USER_CACHE;
import static software.wings.common.Constants.USER_PERMISSION_CACHE;
import static software.wings.common.Constants.WHITELIST_CACHE;

import com.google.inject.Singleton;

import software.wings.beans.User;
import software.wings.beans.security.access.WhitelistConfig;
import software.wings.security.UserPermissionInfo;
import software.wings.service.impl.newrelic.NewRelicApplication.NewRelicApplications;

import java.util.Optional;
import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;

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
    return getCache(USER_CACHE, String.class, User.class, AccessedExpiryPolicy.factoryOf(Duration.THIRTY_MINUTES));
  }

  public Cache<String, NewRelicApplications> getNewRelicApplicationCache() {
    return getCache(NEW_RELIC_APPLICATION_CACHE, String.class, NewRelicApplications.class,
        AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  public Cache<String, UserPermissionInfo> getUserPermissionInfoCache() {
    return getCache(USER_PERMISSION_CACHE, String.class, UserPermissionInfo.class,
        AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  public Cache<String, WhitelistConfig> getWhitelistConfigCache() {
    return getCache(
        WHITELIST_CACHE, String.class, WhitelistConfig.class, AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  public void resetAllCaches() {
    getUserCache().clear();
    getUserPermissionInfoCache().clear();
    getWhitelistConfigCache().clear();
  }
}
