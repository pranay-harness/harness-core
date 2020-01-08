package software.wings.beans.execution;

import lombok.Builder;
import lombok.Value;

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
