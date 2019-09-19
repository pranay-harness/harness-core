package io.harness.limits.defaults.service;

import io.harness.limits.ActionType;
import io.harness.limits.lib.Limit;

public interface DefaultLimitsService {
  // GraphQL external/internal API call's default rate limit constants.
  int GRAPHQL_EXTERNAL_RATE_LIMIT_ACCOUNT_DEFAULT = 30;
  int GRAPHQL_EXTERNAL_RATE_LIMIT_COMMUNITY_ACCOUNT_DEFAULT = 5;
  int GRAPHQL_INTERNAL_RATE_LIMIT_ACCOUNT_DEFAULT = 60;
  int GRAPHQL_INTERNAL_RATE_LIMIT_COMMUNITY_ACCOUNT_DEFAULT = 5;

  int GRAPHQL_RATE_LIMIT_DURATION_IN_MINUTE = 1;

  Limit get(ActionType actionType, String accountType);
}
