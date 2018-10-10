package io.harness.limits.configuration;

import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.lib.Limit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface LimitConfigurationService {
  @Nullable ConfiguredLimit get(String accountId, ActionType actionType);

  /**
   * Configure a new limit.
   *
   * @param accountId - the accountId for which the limit is being specified
   * @param actionType - the action type for which to impose this limit
   * @param limit - limit to be imposed
   * @return configuration was successful or now
   */
  boolean configure(String accountId, ActionType actionType, Limit limit);
}
