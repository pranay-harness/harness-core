package io.harness.limits.defaults.service;

import io.harness.limits.ActionType;
import io.harness.limits.lib.Limit;

public interface DefaultLimitsService {
  // GraphQL default rate limit constants.
  int RATE_LIMIT_ACCOUNT_DEFAULT = 30;
  int RATE_LIMIT_DURATION_IN_MINUTE = 1;

  Limit get(ActionType actionType, String accountType);
}
