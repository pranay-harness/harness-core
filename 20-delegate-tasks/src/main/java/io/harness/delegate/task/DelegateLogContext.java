package io.harness.delegate.task;

import io.harness.logging.AutoLogContext;

public class DelegateLogContext extends AutoLogContext {
  public static final String ID = "delegateId";

  public DelegateLogContext(String delegateId, OverrideBehavior behavior) {
    super(ID, delegateId, behavior);
  }
}
