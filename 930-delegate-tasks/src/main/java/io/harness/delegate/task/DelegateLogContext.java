package io.harness.delegate.task;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.logging.AutoLogContext;

@TargetModule(Module._955_DELEGATE_BEANS)
public class DelegateLogContext extends AutoLogContext {
  public static final String ACCOUNT_ID = "accountId";
  public static final String DELEGATE_ID = "delegateId";

  public DelegateLogContext(String delegateId, OverrideBehavior behavior) {
    super(DELEGATE_ID, delegateId, behavior);
  }

  public DelegateLogContext(String accountId, String delegateId, OverrideBehavior behavior) {
    super(NullSafeImmutableMap.<String, String>builder()
              .putIfNotNull(ACCOUNT_ID, accountId)
              .putIfNotNull(DELEGATE_ID, delegateId)
              .build(),
        behavior);
  }
}
