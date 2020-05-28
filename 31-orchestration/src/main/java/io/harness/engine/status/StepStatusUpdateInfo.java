package io.harness.engine.status;

import io.harness.execution.status.NodeExecutionStatus;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class StepStatusUpdateInfo {
  @NonNull String planExecutionId;
  String nodeExecutionId;
  String interruptId;
  NodeExecutionStatus status;
}
