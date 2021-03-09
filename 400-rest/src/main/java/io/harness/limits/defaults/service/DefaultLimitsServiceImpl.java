package io.harness.limits.defaults.service;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.harness.limits.ActionType;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.limits.lib.Limit;
import lombok.AllArgsConstructor;
import lombok.Value;
import software.wings.beans.AccountType;
import software.wings.features.UsersFeature;
import software.wings.features.api.UsageLimitedFeature;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class DefaultLimitsServiceImpl implements DefaultLimitsService {
  private static final Integer MAX_APP_COUNT = 500;

  @Value
  @AllArgsConstructor
  static class LimitKey {
    private ActionType actionType;
    private String accountType;
  }

  private final ImmutableMap<LimitKey, Limit> defaults;

  @Inject
  DefaultLimitsServiceImpl(@Named(UsersFeature.FEATURE_NAME) UsageLimitedFeature usersFeature) {
    Map<LimitKey, Limit> defaultLimits = new HashMap<>();

    // Application Limits
    defaultLimits.put(new LimitKey(ActionType.CREATE_APPLICATION, AccountType.TRIAL), new StaticLimit(MAX_APP_COUNT));
    defaultLimits.put(new LimitKey(ActionType.CREATE_APPLICATION, AccountType.PAID), new StaticLimit(MAX_APP_COUNT));
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_APPLICATION, AccountType.COMMUNITY), new StaticLimit(MAX_APP_COUNT));
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_APPLICATION, AccountType.ESSENTIALS), new StaticLimit(MAX_APP_COUNT));

    // Service Limits
    defaultLimits.put(new LimitKey(ActionType.CREATE_SERVICE, AccountType.TRIAL), new StaticLimit(10));
    defaultLimits.put(new LimitKey(ActionType.CREATE_SERVICE, AccountType.PAID), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(new LimitKey(ActionType.CREATE_SERVICE, AccountType.COMMUNITY), new StaticLimit(10));
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_SERVICE, AccountType.ESSENTIALS), new StaticLimit(MAX_APP_COUNT * 10));

    // Deployment Limits
    defaultLimits.put(new LimitKey(ActionType.DEPLOY, AccountType.TRIAL), new RateLimit(100, 24, TimeUnit.HOURS));
    defaultLimits.put(new LimitKey(ActionType.DEPLOY, AccountType.PAID), new RateLimit(100, 24, TimeUnit.HOURS));
    defaultLimits.put(new LimitKey(ActionType.DEPLOY, AccountType.COMMUNITY), new RateLimit(100, 24, TimeUnit.HOURS));
    defaultLimits.put(new LimitKey(ActionType.DEPLOY, AccountType.ESSENTIALS), new RateLimit(100, 24, TimeUnit.HOURS));

    // User Limits
    defaultLimits.put(new LimitKey(ActionType.CREATE_USER, AccountType.TRIAL),
        new StaticLimit(usersFeature.getMaxUsageAllowed(AccountType.TRIAL)));
    defaultLimits.put(new LimitKey(ActionType.CREATE_USER, AccountType.PAID),
        new StaticLimit(usersFeature.getMaxUsageAllowed(AccountType.PAID)));
    defaultLimits.put(new LimitKey(ActionType.CREATE_USER, AccountType.COMMUNITY),
        new StaticLimit(usersFeature.getMaxUsageAllowed(AccountType.COMMUNITY)));
    defaultLimits.put(new LimitKey(ActionType.CREATE_USER, AccountType.ESSENTIALS),
        new StaticLimit(usersFeature.getMaxUsageAllowed(AccountType.ESSENTIALS)));

    // Pipeline Limits
    defaultLimits.put(new LimitKey(ActionType.CREATE_PIPELINE, AccountType.TRIAL), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(new LimitKey(ActionType.CREATE_PIPELINE, AccountType.PAID), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_PIPELINE, AccountType.COMMUNITY), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_PIPELINE, AccountType.ESSENTIALS), new StaticLimit(MAX_APP_COUNT * 10));

    // Workflow Limits
    defaultLimits.put(new LimitKey(ActionType.CREATE_WORKFLOW, AccountType.TRIAL), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(new LimitKey(ActionType.CREATE_WORKFLOW, AccountType.PAID), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_WORKFLOW, AccountType.COMMUNITY), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_WORKFLOW, AccountType.ESSENTIALS), new StaticLimit(MAX_APP_COUNT * 10));

    // Export executions request limits
    defaultLimits.put(new LimitKey(ActionType.EXPORT_EXECUTIONS_REQUEST, AccountType.TRIAL),
        new StaticLimit(EXPORT_EXECUTIONS_LIMIT_PER_DAY));
    defaultLimits.put(new LimitKey(ActionType.EXPORT_EXECUTIONS_REQUEST, AccountType.PAID),
        new StaticLimit(EXPORT_EXECUTIONS_LIMIT_PER_DAY));
    defaultLimits.put(new LimitKey(ActionType.EXPORT_EXECUTIONS_REQUEST, AccountType.COMMUNITY),
        new StaticLimit(EXPORT_EXECUTIONS_LIMIT_PER_DAY));
    defaultLimits.put(new LimitKey(ActionType.EXPORT_EXECUTIONS_REQUEST, AccountType.ESSENTIALS),
        new StaticLimit(EXPORT_EXECUTIONS_LIMIT_PER_DAY));

    // Infrastructure Provisioner Limits
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_INFRA_PROVISIONER, AccountType.TRIAL), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_INFRA_PROVISIONER, AccountType.PAID), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_INFRA_PROVISIONER, AccountType.COMMUNITY), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_INFRA_PROVISIONER, AccountType.ESSENTIALS), new StaticLimit(MAX_APP_COUNT * 10));

    // GraphQL external API call Limits
    defaultLimits.put(new LimitKey(ActionType.GRAPHQL_CALL, AccountType.TRIAL),
        new RateLimit(
            GRAPHQL_EXTERNAL_RATE_LIMIT_ACCOUNT_DEFAULT, GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE, TimeUnit.MINUTES));
    defaultLimits.put(new LimitKey(ActionType.GRAPHQL_CALL, AccountType.PAID),
        new RateLimit(
            GRAPHQL_EXTERNAL_RATE_LIMIT_ACCOUNT_DEFAULT, GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE, TimeUnit.MINUTES));
    // Use lower limit for Community/Essential account.
    defaultLimits.put(new LimitKey(ActionType.GRAPHQL_CALL, AccountType.COMMUNITY),
        new RateLimit(GRAPHQL_EXTERNAL_RATE_LIMIT_COMMUNITY_ACCOUNT_DEFAULT, GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE,
            TimeUnit.MINUTES));
    defaultLimits.put(new LimitKey(ActionType.GRAPHQL_CALL, AccountType.ESSENTIALS),
        new RateLimit(GRAPHQL_EXTERNAL_RATE_LIMIT_COMMUNITY_ACCOUNT_DEFAULT, GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE,
            TimeUnit.MINUTES));

    // GraphQL internal API call Limits
    defaultLimits.put(new LimitKey(ActionType.GRAPHQL_CUSTOM_DASH_CALL, AccountType.TRIAL),
        new RateLimit(
            GRAPHQL_INTERNAL_RATE_LIMIT_ACCOUNT_DEFAULT, GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE, TimeUnit.MINUTES));
    defaultLimits.put(new LimitKey(ActionType.GRAPHQL_CUSTOM_DASH_CALL, AccountType.PAID),
        new RateLimit(
            GRAPHQL_INTERNAL_RATE_LIMIT_ACCOUNT_DEFAULT, GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE, TimeUnit.MINUTES));
    // Use lower limit for Community/Essential account.
    defaultLimits.put(new LimitKey(ActionType.GRAPHQL_CUSTOM_DASH_CALL, AccountType.COMMUNITY),
        new RateLimit(GRAPHQL_INTERNAL_RATE_LIMIT_COMMUNITY_ACCOUNT_DEFAULT, GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE,
            TimeUnit.MINUTES));
    defaultLimits.put(new LimitKey(ActionType.GRAPHQL_CUSTOM_DASH_CALL, AccountType.ESSENTIALS),
        new RateLimit(GRAPHQL_INTERNAL_RATE_LIMIT_COMMUNITY_ACCOUNT_DEFAULT, GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE,
            TimeUnit.MINUTES));

    defaults = ImmutableMap.copyOf(defaultLimits);
  }

  @Override
  public Limit get(ActionType actionType, String accountType) {
    return defaults.get(new LimitKey(actionType, accountType));
  }
}
