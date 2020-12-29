package io.harness.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.DelegateConfigurationServiceProvider;
import io.harness.delegate.DelegatePropertiesServiceProvider;
import io.harness.managerclient.GetDelegatePropertiesRequest;
import io.harness.managerclient.GetDelegatePropertiesResponse;
import io.harness.managerclient.SecretManagerCacheTTL;
import io.harness.managerclient.SecretManagerCacheTTLQuery;
import io.harness.security.encryption.SecretUniqueIdentifier;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Weigher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@Singleton
@ValidateOnExecution
@OwnedBy(PL)
@Slf4j
public class SecretsDelegateCacheServiceImpl implements SecretsDelegateCacheService {
  private final Cache<SecretUniqueIdentifier, char[]> secretsCache;
  @Inject private DelegateConfigurationServiceProvider delegateConfigurationServiceProvider;
  @Inject private DelegatePropertiesServiceProvider delegatePropertiesServiceProvider;

  public Duration initializeCacheExpiry() {
    try {
      GetDelegatePropertiesRequest delegatePropertiesRequest =
          GetDelegatePropertiesRequest.newBuilder()
              .setAccountId(delegateConfigurationServiceProvider.getAccount())
              .addRequestEntry(Any.pack(SecretManagerCacheTTLQuery.newBuilder().build()))
              .build();

      GetDelegatePropertiesResponse delegatePropertiesResponse =
          delegatePropertiesServiceProvider.getDelegateProperties(delegatePropertiesRequest);
      if (delegatePropertiesResponse != null) {
        long expireCacheTTLFromAccountPref =
            delegatePropertiesResponse.getResponseEntry(0).unpack(SecretManagerCacheTTL.class).getExpireCacheTTL();
        if (expireCacheTTLFromAccountPref > 0) {
          log.info("Fetched SecretsCacheTTL from Account Preferences and set expiry to {} hours",
              expireCacheTTLFromAccountPref);
          return Duration.ofHours(expireCacheTTLFromAccountPref);
        }
      }

    } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
      log.warn("Unable to fetch secretsCacheExpiryTTL from manager for account {}",
          delegateConfigurationServiceProvider.getAccount(), invalidProtocolBufferException);
      return Duration.ofHours(1);
    }
    return Duration.ofHours(1); // This should never execute fall back to default in case it does.
  }

  @Inject
  public SecretsDelegateCacheServiceImpl(DelegatePropertiesServiceProvider delegatePropertiesServiceProvider,
      DelegateConfigurationServiceProvider delegateConfigurationServiceProvider) {
    this.delegatePropertiesServiceProvider = delegatePropertiesServiceProvider;
    this.delegateConfigurationServiceProvider = delegateConfigurationServiceProvider;
    this.secretsCache = Caffeine.newBuilder()
                            .maximumWeight(2 * 1024 * 1024) // 4MB worth of characters
                            .expireAfterAccess(initializeCacheExpiry().toMillis(), TimeUnit.MILLISECONDS)
                            .weigher(new SecretCacheWeigher())
                            .build();
  }

  public static class SecretCacheWeigher implements Weigher<SecretUniqueIdentifier, char[]> {
    @Override
    public int weigh(SecretUniqueIdentifier secretUniqueIdentifier, char[] value) {
      return value.length;
    }
  }

  @Override
  public char[] get(SecretUniqueIdentifier key, Function<SecretUniqueIdentifier, char[]> mappingFunction) {
    try {
      return secretsCache.get(key, mappingFunction);
    } catch (Exception e) {
      log.error("Cache get operation failed unexpectedly", e);
      return mappingFunction.apply(key);
    }
  }

  @Override
  public void put(SecretUniqueIdentifier key, char[] value) {
    try {
      secretsCache.put(key, value);
    } catch (Exception e) {
      log.error("Cache put operation failed unexpectedly", e);
    }
  }

  @Override
  public void remove(SecretUniqueIdentifier key) {
    try {
      secretsCache.invalidate(key);
    } catch (Exception e) {
      log.error("Cache invalidation failed for key {}", key, e);
    }
  }
}
