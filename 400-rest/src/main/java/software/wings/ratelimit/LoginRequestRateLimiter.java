/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.ratelimit;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.lib.RateBasedLimit;

import software.wings.beans.Account;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class LoginRequestRateLimiter {
  private static final int GLOBAL_LOGIN_REQUEST_LIMIT_PER_MINUTE = 300;

  Cache<String, RequestRateLimiter> accountRateLimiterCache =
      Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(15, TimeUnit.MINUTES).build();

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
      log.error("Global Login Request has reached its limit with this API Call from {}", remoteHost);
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
