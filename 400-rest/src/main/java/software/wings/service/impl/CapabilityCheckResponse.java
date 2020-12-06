package software.wings.service.impl;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

public interface CapabilityCheckResponse extends DelegateTaskNotifyResponseData {
  boolean isAbleToExecutePerpetualTask();
}
