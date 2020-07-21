package software.wings.app;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import io.harness.cache.HarnessCacheManager;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.beans.security.access.WhitelistConfig;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRestrictionInfo;
import software.wings.service.impl.newrelic.NewRelicApplication.NewRelicApplications;

import javax.cache.Cache;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;

public class ManagerCacheRegistrar extends AbstractModule {
  public static final String AUTH_TOKEN_CACHE = "authTokenCache";
  public static final String USER_CACHE = "userCache";
  public static final String HARNESS_API_KEY_CACHE = "harnessApiKeyCache";
  public static final String NEW_RELIC_APPLICATION_CACHE = "nrApplicationCache";
  public static final String TRIAL_EMAIL_CACHE = "trialEmailCache";
  public static final String USER_PERMISSION_CACHE = "userPermissionCache";
  public static final String USER_RESTRICTION_CACHE = "userRestrictionCache";
  public static final String APIKEY_CACHE = "apiKeyCache";
  public static final String APIKEY_PERMISSION_CACHE = "apiKeyPermissionCache";
  public static final String APIKEY_RESTRICTION_CACHE = "apiKeyRestrictionCache";
  public static final String WHITELIST_CACHE = "whitelistCache";

  @Provides
  @Named(AUTH_TOKEN_CACHE)
  @Singleton
  public Cache<String, AuthToken> getAuthTokenCache(HarnessCacheManager harnessCacheManager) {
    return harnessCacheManager.getCache(
        AUTH_TOKEN_CACHE, String.class, AuthToken.class, AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
  }

  @Provides
  @Named(HARNESS_API_KEY_CACHE)
  @Singleton
  public Cache<String, String> getHarnessApiKeyCache(HarnessCacheManager harnessCacheManager) {
    return harnessCacheManager.getCache(
        HARNESS_API_KEY_CACHE, String.class, String.class, AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
  }

  @Provides
  @Named(TRIAL_EMAIL_CACHE)
  @Singleton
  public Cache<String, Integer> getTrialRegistrationEmailCache(HarnessCacheManager harnessCacheManager) {
    return harnessCacheManager.getCache(
        TRIAL_EMAIL_CACHE, String.class, Integer.class, AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  @Provides
  @Named(USER_CACHE)
  @Singleton
  public Cache<String, User> getUserCache(HarnessCacheManager harnessCacheManager) {
    return harnessCacheManager.getCache(
        USER_CACHE, String.class, User.class, AccessedExpiryPolicy.factoryOf(Duration.THIRTY_MINUTES));
  }

  @Provides
  @Named(APIKEY_CACHE)
  @Singleton
  public Cache<String, ApiKeyEntry> getApiKeyCache(HarnessCacheManager harnessCacheManager) {
    return harnessCacheManager.getCache(
        APIKEY_CACHE, String.class, ApiKeyEntry.class, AccessedExpiryPolicy.factoryOf(Duration.THIRTY_MINUTES));
  }

  @Provides
  @Named(NEW_RELIC_APPLICATION_CACHE)
  @Singleton
  public Cache<String, NewRelicApplications> getNewRelicApplicationCache(HarnessCacheManager harnessCacheManager) {
    return harnessCacheManager.getCache(NEW_RELIC_APPLICATION_CACHE, String.class, NewRelicApplications.class,
        AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
  }

  @Provides
  @Named(APIKEY_PERMISSION_CACHE)
  @Singleton
  public Cache<String, UserPermissionInfo> getApiKeyPermissionInfoCache(HarnessCacheManager harnessCacheManager) {
    return harnessCacheManager.getCache(APIKEY_PERMISSION_CACHE, String.class, UserPermissionInfo.class,
        AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  @Provides
  @Named(APIKEY_RESTRICTION_CACHE)
  @Singleton
  public Cache<String, UserRestrictionInfo> getApiKeyRestrictionInfoCache(HarnessCacheManager harnessCacheManager) {
    return harnessCacheManager.getCache(APIKEY_RESTRICTION_CACHE, String.class, UserRestrictionInfo.class,
        AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  @Provides
  @Named(WHITELIST_CACHE)
  @Singleton
  public Cache<String, WhitelistConfig> getWhitelistConfigCache(HarnessCacheManager harnessCacheManager) {
    return harnessCacheManager.getCache(
        WHITELIST_CACHE, String.class, WhitelistConfig.class, AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
    bindCaches();
  }

  private void bindCaches() {
    MapBinder<String, Cache<?, ?>> mapBinder =
        MapBinder.newMapBinder(binder(), TypeLiteral.get(String.class), new TypeLiteral<Cache<?, ?>>() {});
    mapBinder.addBinding(AUTH_TOKEN_CACHE).to(Key.get(new TypeLiteral<Cache<String, AuthToken>>() {
    }, Names.named(AUTH_TOKEN_CACHE)));
    mapBinder.addBinding(HARNESS_API_KEY_CACHE).to(Key.get(new TypeLiteral<Cache<String, String>>() {
    }, Names.named(HARNESS_API_KEY_CACHE)));
    mapBinder.addBinding(TRIAL_EMAIL_CACHE).to(Key.get(new TypeLiteral<Cache<String, Integer>>() {
    }, Names.named(TRIAL_EMAIL_CACHE)));
    mapBinder.addBinding(USER_CACHE).to(Key.get(new TypeLiteral<Cache<String, User>>() {}, Names.named(USER_CACHE)));
    mapBinder.addBinding(APIKEY_CACHE).to(Key.get(new TypeLiteral<Cache<String, ApiKeyEntry>>() {
    }, Names.named(APIKEY_CACHE)));
    mapBinder.addBinding(NEW_RELIC_APPLICATION_CACHE)
        .to(Key.get(
            new TypeLiteral<Cache<String, NewRelicApplications>>() {}, Names.named(NEW_RELIC_APPLICATION_CACHE)));
    mapBinder.addBinding(APIKEY_PERMISSION_CACHE).to(Key.get(new TypeLiteral<Cache<String, UserPermissionInfo>>() {
    }, Names.named(APIKEY_PERMISSION_CACHE)));
    mapBinder.addBinding(APIKEY_RESTRICTION_CACHE).to(Key.get(new TypeLiteral<Cache<String, UserRestrictionInfo>>() {
    }, Names.named(APIKEY_RESTRICTION_CACHE)));
    mapBinder.addBinding(WHITELIST_CACHE).to(Key.get(new TypeLiteral<Cache<String, WhitelistConfig>>() {
    }, Names.named(WHITELIST_CACHE)));
  }

  private void registerRequiredBindings() {
    requireBinding(HarnessCacheManager.class);
  }
}
