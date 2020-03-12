package software.wings.ratelimit;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import io.harness.configuration.DeployMode;
import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.lib.RateBasedLimit;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DelegateRequestRateLimiter {
  private static final int GLOBAL_DELEGATE_REQUEST_LIMIT_PER_MINUTE = 10000;
  private static final int ACCOUNT_PER_DELEGATE_REQUEST_LIMIT_PER_MINUTE = 200;

  LimitConfigurationService limitConfigurationService;

  String deployMode = System.getenv(DeployMode.DEPLOY_MODE);

  Cache<String, RequestRateLimiter> accountRateLimiterCache =
      Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(30, TimeUnit.MINUTES).build();

  RequestRateLimiter globalRateLimiter;

  @Inject
  DelegateRequestRateLimiter(@NotNull LimitConfigurationService limitConfigurationService) {
    this.limitConfigurationService = limitConfigurationService;

    globalRateLimiter = new InMemorySlidingWindowRequestRateLimiter(
        RequestLimitRule.of(Duration.ofMinutes(1), GLOBAL_DELEGATE_REQUEST_LIMIT_PER_MINUTE));
  }

  public boolean isOverRateLimit(String accountId, String delegateId) {
    if (DeployMode.isOnPrem(deployMode)) {
      return false;
    } else {
      boolean globalRateLimitReached = globalRateLimiter.overLimitWhenIncremented(Account.GLOBAL_ACCOUNT_ID);
      if (globalRateLimitReached) {
        logger.info("Global Delegate Acquire Task limit reached");
        return true;
      } else if (isNotEmpty(accountId)) {
        boolean rateLimitReached = getAccountRateLimiter(accountId).overLimitWhenIncremented(delegateId);
        if (rateLimitReached) {
          logger.info("Delegate Acquire Task limit reached");
          return true;
        }
      }
      return false;
    }
  }

  private RequestRateLimiter getAccountRateLimiter(String accountId) {
    return accountRateLimiterCache.get(accountId, key -> getAccountRateLimiterInternal(accountId));
  }

  private RequestRateLimiter getAccountRateLimiterInternal(String accountId) {
    ConfiguredLimit<RateBasedLimit> configuredLimit =
        limitConfigurationService.getOrDefault(accountId, ActionType.DELEGATE_ACQUIRE_TASK);
    if (configuredLimit == null) {
      return new InMemorySlidingWindowRequestRateLimiter(
          RequestLimitRule.of(Duration.ofMinutes(1), ACCOUNT_PER_DELEGATE_REQUEST_LIMIT_PER_MINUTE));
    } else {
      RateBasedLimit rateBasedLimit = configuredLimit.getLimit();
      return new InMemorySlidingWindowRequestRateLimiter(RequestLimitRule.of(
          Duration.ofSeconds(rateBasedLimit.getDurationUnit().toSeconds(rateBasedLimit.getDuration())),
          rateBasedLimit.getCount()));
    }
  }
}
