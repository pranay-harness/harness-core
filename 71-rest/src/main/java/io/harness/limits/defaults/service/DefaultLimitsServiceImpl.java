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
    defaultLimits.put(new LimitKey(ActionType.CREATE_APPLICATION, AccountType.TRIAL), new StaticLimit(1000));
    defaultLimits.put(new LimitKey(ActionType.CREATE_APPLICATION, AccountType.PAID), new StaticLimit(1000));
    defaultLimits.put(new LimitKey(ActionType.CREATE_APPLICATION, AccountType.FREEMIUM), new StaticLimit(1000));

    // Deployment Limits
    defaultLimits.put(new LimitKey(ActionType.DEPLOY, AccountType.TRIAL), new RateLimit(1000, 24, TimeUnit.HOURS));
    defaultLimits.put(new LimitKey(ActionType.DEPLOY, AccountType.PAID), new RateLimit(1000, 24, TimeUnit.HOURS));
    defaultLimits.put(new LimitKey(ActionType.DEPLOY, AccountType.FREEMIUM), new RateLimit(1000, 24, TimeUnit.HOURS));

    defaults = ImmutableMap.copyOf(defaultLimits);
  }

  @Override
  public Limit get(ActionType actionType, String accountType) {
    return defaults.get(new LimitKey(actionType, accountType));
  }
}
