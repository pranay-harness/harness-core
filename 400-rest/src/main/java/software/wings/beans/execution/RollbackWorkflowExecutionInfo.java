package software.wings.beans.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@TargetModule(HarnessModule._957_CG_BEANS)
public class RollbackWorkflowExecutionInfo {
  RollbackType rollbackType;
  String rollbackStateExecutionId;
  Long rollbackStartTs;
  Long rollbackDuration;
}
