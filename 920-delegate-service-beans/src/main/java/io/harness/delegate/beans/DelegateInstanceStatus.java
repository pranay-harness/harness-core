package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
public enum DelegateInstanceStatus {
  ENABLED,
  WAITING_FOR_APPROVAL,
  @Deprecated DISABLED,
  DELETED
}
