package io.harness.delegate.task.gcp.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GcpValidationTaskResponse implements GcpResponse {
  private CommandExecutionStatus executionStatus;
  private String errorMessage;
  private DelegateMetaInfo delegateMetaInfo;
}
