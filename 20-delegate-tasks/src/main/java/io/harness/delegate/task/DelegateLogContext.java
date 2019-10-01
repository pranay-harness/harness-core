package io.harness.delegate.task;

import io.harness.logging.AutoLogContext;

public class DelegateLogContext extends AutoLogContext {
  public DelegateLogContext(String delegateId, OverrideBehavior behavior) {
    super("delegateId", delegateId, behavior);
  }
}
