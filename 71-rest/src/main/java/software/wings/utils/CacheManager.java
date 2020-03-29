package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.beans.security.access.WhitelistConfig;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRestrictionInfo;
import software.wings.service.impl.newrelic.NewRelicApplication.NewRelicApplications;

import java.util.Optional;
import java.util.Set;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;

@Singleton
public class CacheManager {
  public static final String USER_CACHE = "userCache";
  private static final String HARNESS_API_KEY_CACHE = "harnessApiKeyCache";
  private static final String NEW_RELIC_APPLICATION_CACHE = "nrApplicationCache";
  private static final String TRIAL_EMAIL_CACHE = "trialEmailCache";
  private static final String USER_PERMISSION_CACHE = "userPermissionCache";
  private static final String USER_RESTRICTION_CACHE = "userRestrictionCache";
  private static final String APIKEY_CACHE = "apiKeyCache";
  private static final String APIKEY_PERMISSION_CACHE = "apiKeyPermissionCache";
  private static final String APIKEY_RESTRICTION_CACHE = "apiKeyRestrictionCache";
  private static final String WHITELIST_CACHE = "whitelistCache";
  private static final String AUTH_TOKEN_CACHE = "authTokenCache";

  @Value
  @Builder
  public static class CacheManagerConfig {
    Set<String> disabled;
  }

  @Inject CacheManagerConfig config;

  public <K, V> Cache<K, V> getCache(
      String cacheName, Class<K> keyType, Class<V> valueType, Factory<ExpiryPolicy> expiryPolicy) {
    if (isNotEmpty(config.getDisabled()) && config.getDisabled().contains(cacheName)) {
      return null;
    }

    MutableConfiguration<K, V> configuration = new MutableConfiguration<>();
    configuration.setTypes(keyType, valueType);
    configuration.setStoreByValue(true);
    configuration.setExpiryPolicyFactory(expiryPolicy);

    try {
      return Optional.ofNullable(Caching.getCache(cacheName, keyType, valueType))
          .orElseGet(() -> Caching.getCachingProvider().getCacheManager().createCache(cacheName, configuration));
    } catch (CacheException ce) {
      if (ce.getMessage().equalsIgnoreCase("A cache named " + cacheName + " already exists.")) {
        return Caching.getCache(cacheName, keyType, valueType);
      }
      throw ce;
    }
  }

  public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
    return getCache(cacheName, keyType, valueType, EternalExpiryPolicy.factoryOf());
  }

  public Cache<String, AuthToken> getAuthTokenCache() {
    return getCache(
        AUTH_TOKEN_CACHE, String.class, AuthToken.class, AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
  }

  public Cache<String, String> getHarnessApiKeyCache() {
    return getCache(
        HARNESS_API_KEY_CACHE, String.class, String.class, AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
  }

  public Cache<String, Integer> getTrialRegistrationEmailCache() {
    return getCache(TRIAL_EMAIL_CACHE, String.class, Integer.class, AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  public Cache<String, User> getUserCache() {
    return getCache(USER_CACHE, String.class, User.class, AccessedExpiryPolicy.factoryOf(Duration.THIRTY_MINUTES));
  }

  public Cache<String, ApiKeyEntry> getApiKeyCache() {
    return getCache(
        APIKEY_CACHE, String.class, ApiKeyEntry.class, AccessedExpiryPolicy.factoryOf(Duration.THIRTY_MINUTES));
  }

  public Cache<String, NewRelicApplications> getNewRelicApplicationCache() {
    return getCache(NEW_RELIC_APPLICATION_CACHE, String.class, NewRelicApplications.class,
        AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
  }

  public Cache<String, UserPermissionInfo> getUserPermissionInfoCache() {
    return getCache(USER_PERMISSION_CACHE, String.class, UserPermissionInfo.class,
        AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  public Cache<String, UserRestrictionInfo> getUserRestrictionInfoCache() {
    return getCache(USER_RESTRICTION_CACHE, String.class, UserRestrictionInfo.class,
        AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  public Cache<String, UserPermissionInfo> getApiKeyPermissionInfoCache() {
    return getCache(APIKEY_PERMISSION_CACHE, String.class, UserPermissionInfo.class,
        AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  public Cache<String, UserRestrictionInfo> getApiKeyRestrictionInfoCache() {
    return getCache(APIKEY_RESTRICTION_CACHE, String.class, UserRestrictionInfo.class,
        AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  public Cache<String, WhitelistConfig> getWhitelistConfigCache() {
    return getCache(
        WHITELIST_CACHE, String.class, WhitelistConfig.class, AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  public void resetAllCaches() {
    Cache<String, User> userCache = getUserCache();
    if (userCache != null) {
      userCache.clear();
    }

    Cache<String, ApiKeyEntry> apiKeyCache = getApiKeyCache();
    if (apiKeyCache != null) {
      apiKeyCache.clear();
    }

    Cache<String, UserPermissionInfo> userPermissionInfoCache = getUserPermissionInfoCache();
    if (userPermissionInfoCache != null) {
      userPermissionInfoCache.clear();
    }

    Cache<String, UserRestrictionInfo> userRestrictionInfoCache = getUserRestrictionInfoCache();
    if (userRestrictionInfoCache != null) {
      userRestrictionInfoCache.clear();
    }

    Cache<String, UserPermissionInfo> apiKeyPermissionInfoCache = getApiKeyPermissionInfoCache();
    if (apiKeyPermissionInfoCache != null) {
      apiKeyPermissionInfoCache.clear();
    }

    Cache<String, UserRestrictionInfo> apiKeyRestrictionInfoCache = getApiKeyRestrictionInfoCache();
    if (apiKeyRestrictionInfoCache != null) {
      apiKeyRestrictionInfoCache.clear();
    }

    Cache<String, WhitelistConfig> whitelistConfigCache = getWhitelistConfigCache();
    if (whitelistConfigCache != null) {
      whitelistConfigCache.clear();
    }

    Cache<String, AuthToken> authTokenCache = getAuthTokenCache();
    if (authTokenCache != null) {
      authTokenCache.clear();
    }
  }
}
