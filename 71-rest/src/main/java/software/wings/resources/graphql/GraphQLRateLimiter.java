package software.wings.resources.graphql;

import static io.harness.limits.defaults.service.DefaultLimitsService.GRAPHQL_EXTERNAL_RATE_LIMIT_ACCOUNT_DEFAULT;
import static io.harness.limits.defaults.service.DefaultLimitsService.GRAPHQL_INTERNAL_RATE_LIMIT_ACCOUNT_DEFAULT;
import static io.harness.limits.defaults.service.DefaultLimitsService.GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import io.harness.configuration.DeployMode;
import io.harness.data.structure.EmptyPredicate;
import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.lib.RateBasedLimit;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

/**
 * @author marklu on 9/13/19
 */
@Slf4j
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GraphQLRateLimiter {
  MainConfiguration mainConfiguration;
  LimitConfigurationService limitConfigurationService;

  RequestRateLimiter defaultAccountExternalRequestRateLimiter;
  RequestRateLimiter defaultAccountInternalRequestRateLimiter;

  String deployMode = System.getenv(DeployMode.DEPLOY_MODE);

  // PL-3447: account-level rate limiter limit will be refreshed every 10 minutes.
  // This means account-level rate limiter changes will take effect after 10 minutes.
  Cache<String, RequestRateLimiter> rateLimiterCache =
      Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(10, TimeUnit.MINUTES).build();

  Cache<String, RequestRateLimiter> globalRateLimiterCache =
      Caffeine.newBuilder().maximumSize(10).expireAfterWrite(10, TimeUnit.MINUTES).build();

  @Inject
  GraphQLRateLimiter(
      @NotNull MainConfiguration mainConfiguration, @NotNull LimitConfigurationService limitConfigurationService) {
    this.mainConfiguration = mainConfiguration;
    this.limitConfigurationService = limitConfigurationService;

    defaultAccountExternalRequestRateLimiter =
        new InMemorySlidingWindowRequestRateLimiter(Collections.singleton(RequestLimitRule.of(
            Duration.ofMinutes(GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE), GRAPHQL_EXTERNAL_RATE_LIMIT_ACCOUNT_DEFAULT)));
    defaultAccountInternalRequestRateLimiter =
        new InMemorySlidingWindowRequestRateLimiter(Collections.singleton(RequestLimitRule.of(
            Duration.ofMinutes(GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE), GRAPHQL_INTERNAL_RATE_LIMIT_ACCOUNT_DEFAULT)));
  }

  public boolean isOverApiRateLimit(String accountId, boolean isInternalGraphQLCall) {
    if (DeployMode.isOnPrem(deployMode)) {
      // No GraphQL rate limit for on-prem installation
      return false;
    } else {
      // PL-3447: Need to check both global/across-account rate limit and account-level limit.
      boolean globalRateLimitReached =
          getGlobalRateLimiter(isInternalGraphQLCall).overLimitWhenIncremented(Account.GLOBAL_ACCOUNT_ID);
      String adj = isInternalGraphQLCall ? "Internal" : "External";
      if (globalRateLimitReached) {
        logger.info("Global {} GraphQL API call rate limit reached", adj);
        return true;
      } else if (EmptyPredicate.isNotEmpty(accountId)) {
        boolean accountRateLimitReached =
            getRateLimiterForAccount(accountId, isInternalGraphQLCall).overLimitWhenIncremented(accountId);
        if (accountRateLimitReached) {
          logger.info("Account level {} GraphQL API call rate limit reached for account {}", adj, accountId);
          return true;
        }
      }
      return false;
    }
  }

  RequestRateLimiter getGlobalRateLimiter(final boolean isInternalGraphQLCall) {
    String cacheKey = Account.GLOBAL_ACCOUNT_ID + isInternalGraphQLCall;
    return globalRateLimiterCache.get(cacheKey, key -> {
      ActionType actionType = isInternalGraphQLCall ? ActionType.GRAPHQL_CUSTOM_DASH_CALL : ActionType.GRAPHQL_CALL;
      String adj = isInternalGraphQLCall ? "Internal" : "External";
      int globalGraphQLRateLimitFromEnvironment = isInternalGraphQLCall
          ? mainConfiguration.getPortal().getCustomDashGraphQLRateLimitPerMinute()
          : mainConfiguration.getPortal().getExternalGraphQLRateLimitPerMinute();

      // No default configured for global rate limit in LimitConfigurationService, can use `getOrDefault` call.
      ConfiguredLimit<RateBasedLimit> configuredLimit =
          limitConfigurationService.get(Account.GLOBAL_ACCOUNT_ID, actionType);
      if (configuredLimit == null) {
        logger.info("Global {} GraphQL rate limit from environment configuration is {} calls per minute", adj,
            globalGraphQLRateLimitFromEnvironment);
        return new InMemorySlidingWindowRequestRateLimiter(Collections.singleton(RequestLimitRule.of(
            Duration.ofMinutes(GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE), globalGraphQLRateLimitFromEnvironment)));
      } else {
        RateBasedLimit rateBasedLimit = configuredLimit.getLimit();
        logger.info(
            "Configured global {} GraphQL rate limiter in MongoDB is {} calls in {} {}, it overrides the rate limit of {} calls in {} {} set from environment ",
            adj, rateBasedLimit.getCount(), rateBasedLimit.getDuration(), rateBasedLimit.getDurationUnit(),
            globalGraphQLRateLimitFromEnvironment, 1, TimeUnit.MINUTES);
        return new InMemorySlidingWindowRequestRateLimiter(Collections.singleton(RequestLimitRule.of(
            Duration.ofSeconds(rateBasedLimit.getDurationUnit().toSeconds(rateBasedLimit.getDuration())),
            rateBasedLimit.getCount())));
      }
    });
  }

  RequestRateLimiter getRateLimiterForAccount(final String accountId, final boolean internal) {
    String cacheKey = accountId + internal;
    return rateLimiterCache.get(cacheKey, key -> getRateLimiterForAccountInternal(accountId, internal));
  }

  RequestRateLimiter getRateLimiterForAccountInternal(String accountId, boolean isInternalGraphQLCall) {
    logger.info("Rate limiter cache size: {}", rateLimiterCache.estimatedSize());
    String adj = isInternalGraphQLCall ? "Internal" : "External";
    ActionType actionType = isInternalGraphQLCall ? ActionType.GRAPHQL_CUSTOM_DASH_CALL : ActionType.GRAPHQL_CALL;
    RequestRateLimiter defaultAccountRequestLimiter =
        isInternalGraphQLCall ? defaultAccountInternalRequestRateLimiter : defaultAccountExternalRequestRateLimiter;

    ConfiguredLimit<RateBasedLimit> configuredLimit = limitConfigurationService.getOrDefault(accountId, actionType);
    if (configuredLimit == null) {
      logger.info("Return the default account-level {} GraphQL rate limiter for account {}", adj, accountId);
      return defaultAccountRequestLimiter;
    } else {
      RateBasedLimit rateBasedLimit = configuredLimit.getLimit();
      logger.info("Return the configured {} GraphQL rate limiter for account {} with call count {} in {} {}", adj,
          accountId, rateBasedLimit.getCount(), rateBasedLimit.getDuration(), rateBasedLimit.getDurationUnit());

      if (useDefaultAccountRateLimiter(rateBasedLimit, isInternalGraphQLCall)) {
        return defaultAccountRequestLimiter;
      } else {
        return new InMemorySlidingWindowRequestRateLimiter(Collections.singleton(RequestLimitRule.of(
            Duration.ofSeconds(rateBasedLimit.getDurationUnit().toSeconds(rateBasedLimit.getDuration())),
            rateBasedLimit.getCount())));
      }
    }
  }

  private boolean useDefaultAccountRateLimiter(RateBasedLimit rateBasedLimit, boolean isInternalGraphQLCall) {
    int defaultAccountRateLimit = isInternalGraphQLCall ? GRAPHQL_INTERNAL_RATE_LIMIT_ACCOUNT_DEFAULT
                                                        : GRAPHQL_EXTERNAL_RATE_LIMIT_ACCOUNT_DEFAULT;
    return rateBasedLimit.getCount() == defaultAccountRateLimit
        && rateBasedLimit.getDuration() == GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE
        && rateBasedLimit.getDurationUnit() == TimeUnit.MINUTES;
  }
}
