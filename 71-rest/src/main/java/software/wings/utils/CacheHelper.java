package software.wings.utils;

import com.google.inject.Singleton;

import software.wings.beans.User;
import software.wings.beans.security.access.WhitelistConfig;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRestrictionInfo;
import software.wings.service.impl.newrelic.NewRelicApplication.NewRelicApplications;

import java.util.Optional;
import javax.cache.Cache;
import javax.cache.CacheException;
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
  /**
   * A cache to map api key client type to api key value;
   */
  private static final String HARNESS_API_KEY_CACHE = "harnessApiKeyCache";

  /**
   * The constant NEW_RELIC_APPLICATION_CACHE.
   */
  private static final String NEW_RELIC_APPLICATION_CACHE = "nrApplicationCache";

  /**
   * A cache to track the trial registration email for rate limiting purpose.
   */
  private static final String TRIAL_EMAIL_CACHE = "trialEmailCache";

  /**
   * The constant USER_CACHE.
   */
  public static final String USER_CACHE = "userCache";

  /**
   * The constant USER_PERMISSION_CACHE.
   */
  private static final String USER_PERMISSION_CACHE = "userPermissionCache";

  /**
   * The constant USER_RESTRICTION_CACHE.
   */
  private static final String USER_RESTRICTION_CACHE = "userRestrictionCache";

  /**
   * The constant
   */
  private static final String WHITELIST_CACHE = "whitelistCache";

  public <K, V> Cache<K, V> getCache(
      String cacheName, Class<K> keyType, Class<V> valueType, Factory<ExpiryPolicy> expiryPolicy) {
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
