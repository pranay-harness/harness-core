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
public class WorkflowExecutionInfo {
  String name;
  String executionId;
  private Long startTs;
  RollbackWorkflowExecutionInfo rollbackWorkflowExecutionInfo;
  String accountId;
  String appId;
  String workflowId;
}
