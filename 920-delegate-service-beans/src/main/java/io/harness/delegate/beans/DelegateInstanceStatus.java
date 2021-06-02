package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public enum DelegateInstanceStatus {
  ENABLED,
  WAITING_FOR_APPROVAL,
  @Deprecated DISABLED,
  DELETED
}
