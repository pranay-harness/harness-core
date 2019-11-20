package software.wings.ratelimit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
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
public class LoginRequestRateLimiter {
  private static final int GLOBAL_LOGIN_REQUEST_LIMIT_PER_MINUTE = 300;

  Cache<String, RequestRateLimiter> accountRateLimiterCache =
      Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(30, TimeUnit.MINUTES).build();

  RequestRateLimiter globalRateLimiter;
  LimitConfigurationService limitConfigurationService;

  @Inject
  public LoginRequestRateLimiter(@NotNull LimitConfigurationService limitConfigurationService) {
    this.limitConfigurationService = limitConfigurationService;
  }

  public boolean isOverRateLimit(String remoteHost) {
    boolean globalRateLimitReached =
        getAccountRateLimiter(Account.GLOBAL_ACCOUNT_ID).overLimitWhenIncremented(remoteHost);
    if (globalRateLimitReached) {
      logger.warn("Global Login Request limit reached with this API Call from {}", remoteHost);
      return true;
    }
    return false;
  }

  private RequestRateLimiter getAccountRateLimiter(String accountId) {
    return accountRateLimiterCache.get(accountId, key -> getAccountRateLimiterInternal(accountId));
  }

  private RequestRateLimiter getAccountRateLimiterInternal(String accountId) {
    ConfiguredLimit<RateBasedLimit> configuredLimit =
        limitConfigurationService.getOrDefault(accountId, ActionType.LOGIN_REQUEST_TASK);
    if (configuredLimit == null) {
      return new InMemorySlidingWindowRequestRateLimiter(
          RequestLimitRule.of(Duration.ofMinutes(1), GLOBAL_LOGIN_REQUEST_LIMIT_PER_MINUTE));
    } else {
      RateBasedLimit rateBasedLimit = configuredLimit.getLimit();
      return new InMemorySlidingWindowRequestRateLimiter(
          RequestLimitRule.of(Duration.ofMinutes(rateBasedLimit.getDuration()), rateBasedLimit.getCount()));
    }
  }
}
