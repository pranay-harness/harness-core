package software.wings.beans.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class RollbackWorkflowExecutionInfo {
  RollbackType rollbackType;
  String rollbackStateExecutionId;
  Long rollbackStartTs;
  Long rollbackDuration;
}
