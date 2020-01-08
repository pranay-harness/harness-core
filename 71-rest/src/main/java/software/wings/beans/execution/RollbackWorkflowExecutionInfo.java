package software.wings.beans.execution;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RollbackWorkflowExecutionInfo {
  RollbackType rollbackType;
  String rollbackStateExecutionId;
  Long rollbackStartTs;
  Long rollbackDuration;
}
