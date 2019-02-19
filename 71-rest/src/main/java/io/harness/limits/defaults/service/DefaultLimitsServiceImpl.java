package io.harness.limits.defaults.service;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import io.harness.limits.ActionType;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.limits.lib.Limit;
import lombok.AllArgsConstructor;
import lombok.Value;
import software.wings.beans.AccountType;

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

  DefaultLimitsServiceImpl() {
    Map<LimitKey, Limit> defaultLimits = new HashMap<>();

    // Application Limits
    defaultLimits.put(new LimitKey(ActionType.CREATE_APPLICATION, AccountType.TRIAL), new StaticLimit(MAX_APP_COUNT));
    defaultLimits.put(new LimitKey(ActionType.CREATE_APPLICATION, AccountType.PAID), new StaticLimit(MAX_APP_COUNT));
    defaultLimits.put(new LimitKey(ActionType.CREATE_APPLICATION, AccountType.FREE), new StaticLimit(MAX_APP_COUNT));

    // Deployment Limits
    defaultLimits.put(new LimitKey(ActionType.DEPLOY, AccountType.TRIAL), new RateLimit(100, 24, TimeUnit.HOURS));
    defaultLimits.put(new LimitKey(ActionType.DEPLOY, AccountType.PAID), new RateLimit(100, 24, TimeUnit.HOURS));
    defaultLimits.put(new LimitKey(ActionType.DEPLOY, AccountType.FREE), new RateLimit(5, 24, TimeUnit.HOURS));

    // User Limits
    defaultLimits.put(new LimitKey(ActionType.CREATE_USER, AccountType.TRIAL), new StaticLimit(MAX_APP_COUNT * 3));
    defaultLimits.put(new LimitKey(ActionType.CREATE_USER, AccountType.PAID), new StaticLimit(MAX_APP_COUNT * 3));
    defaultLimits.put(new LimitKey(ActionType.CREATE_USER, AccountType.FREE), new StaticLimit(MAX_APP_COUNT * 3));

    // Pipeline Limits
    defaultLimits.put(new LimitKey(ActionType.CREATE_PIPELINE, AccountType.TRIAL), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(new LimitKey(ActionType.CREATE_PIPELINE, AccountType.PAID), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(new LimitKey(ActionType.CREATE_PIPELINE, AccountType.FREE), new StaticLimit(MAX_APP_COUNT * 10));

    // Workflow Limits
    defaultLimits.put(new LimitKey(ActionType.CREATE_WORKFLOW, AccountType.TRIAL), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(new LimitKey(ActionType.CREATE_WORKFLOW, AccountType.PAID), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(new LimitKey(ActionType.CREATE_WORKFLOW, AccountType.FREE), new StaticLimit(MAX_APP_COUNT * 10));

    // Infrastructure Provisioner Limits
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_INFRA_PROVISIONER, AccountType.TRIAL), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_INFRA_PROVISIONER, AccountType.PAID), new StaticLimit(MAX_APP_COUNT * 10));
    defaultLimits.put(
        new LimitKey(ActionType.CREATE_INFRA_PROVISIONER, AccountType.FREE), new StaticLimit(MAX_APP_COUNT * 10));

    defaults = ImmutableMap.copyOf(defaultLimits);
  }

  @Override
  public Limit get(ActionType actionType, String accountType) {
    return defaults.get(new LimitKey(actionType, accountType));
  }
}
