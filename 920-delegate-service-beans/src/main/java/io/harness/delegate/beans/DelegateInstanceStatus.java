package io.harness.delegate.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._920_DELEGATE_SERVICE_BEANS)
public enum DelegateInstanceStatus {
  ENABLED,
  WAITING_FOR_APPROVAL,
  @Deprecated DISABLED,
  DELETED
}
