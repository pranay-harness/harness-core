package software.wings.beans.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class WorkflowExecutionInfo {
  String name;
  String executionId;
  private Long startTs;
  RollbackWorkflowExecutionInfo rollbackWorkflowExecutionInfo;
  String accountId;
  String appId;
  String workflowId;
}
