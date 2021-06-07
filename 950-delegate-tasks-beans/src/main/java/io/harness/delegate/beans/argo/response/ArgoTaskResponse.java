package io.harness.delegate.beans.argo.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.logging.CommandExecutionStatus;

@OwnedBy(CDP)
public interface ArgoTaskResponse extends DelegateTaskNotifyResponseData {
  CommandExecutionStatus getExecutionStatus();
  String getErrorMessage();
}
