package io.harness.delegate.task.gcp.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.dto.ErrorDetail;

@OwnedBy(CDP)
public interface GcpTaskResponse extends GcpResponse {
  CommandExecutionStatus getCommandExecutionStatus();
  String getErrorMessage();
  ErrorDetail getErrorDetail();
}
