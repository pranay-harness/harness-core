package io.harness.delegate.beans;

import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.logging.AutoLogContext;

public class DelegateParamsLogContext extends AutoLogContext {
  public static final String ACCOUNT_ID = "accountId";
  public static final String DELEGATE_PARAMS = "delegateParams";

  public DelegateParamsLogContext(String accountId, String delegateParams, OverrideBehavior behavior) {
    super(NullSafeImmutableMap.<String, String>builder()
              .putIfNotNull(ACCOUNT_ID, accountId)
              .putIfNotNull(DELEGATE_PARAMS, delegateParams)
              .build(),
        behavior);
  }
}
