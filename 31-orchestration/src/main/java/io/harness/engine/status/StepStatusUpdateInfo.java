package io.harness.engine.status;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.Status;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class StepStatusUpdateInfo {
  @NonNull String planExecutionId;
  String nodeExecutionId;
  String interruptId;
  Status status;
}
