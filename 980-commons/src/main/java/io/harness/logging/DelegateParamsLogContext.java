package io.harness.logging;

import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.delegate.beans.DelegateParams;

public class DelegateParamsLogContext extends AutoLogContext {
  public static final String ACCOUNT_ID = "accountId";
  public static final String DELEGATE_PARAMS = "delegateParams";

  public DelegateParamsLogContext(String accountId, DelegateParams delegateParams, OverrideBehavior behavior) {
    super(NullSafeImmutableMap.<String, String>builder()
              .putIfNotNull(ACCOUNT_ID, accountId)
              .putIfNotNull(DELEGATE_PARAMS, delegateParams)
              .build(),
        behavior);
  }
}
