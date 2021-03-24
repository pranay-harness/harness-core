package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

@OwnedBy(DEL)
public interface CapabilityCheckResponse extends DelegateTaskNotifyResponseData {
  boolean isAbleToExecutePerpetualTask();
}
