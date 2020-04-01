package io.harness.limits.configuration;

import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.lib.Limit;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface LimitConfigurationService extends OwnedByAccount {
  /**
   * First tries to get a limit configured with given accountId.
   * If no limit is configured for this accountId, then it gets a "default limit"
   *
   * @param accountId the account for which to fetch the limit
   * @param actionType action for which to fetch the limit
   * @return configured limit, or null if no limit is configured for this account and there is no default.
   */
  @Nullable ConfiguredLimit getOrDefault(String accountId, ActionType actionType);

  /**
   * Gt a limit configured with given accountId without resorting to default limits.
   *
   * @param accountId the account for which to fetch the limit
   * @param actionType action for which to fetch the limit
   * @return configured limit, or null if no limit is configured for this account and there is no default.
   */
  @Nullable ConfiguredLimit get(String accountId, ActionType actionType);

  List<List<ConfiguredLimit>> getAllLimitsConfiguredForAccounts(List<String> accountIds);

  List<ConfiguredLimit> getLimitsConfiguredForAccount(String accountId);

  /**
   * Configure a new limit.
   *
   * @param accountId  the accountId for which the limit is being specified
   * @param actionType the action type for which to impose this limit
   * @param limit      limit to be imposed
   * @return configuration was successful or now
   */
  boolean configure(String accountId, ActionType actionType, Limit limit);
}
