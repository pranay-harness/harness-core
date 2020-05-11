package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;
import software.wings.beans.WorkflowExecution;

@OwnedBy(CDC)
public class WorkflowExecutionLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(WorkflowExecution.class);

  public WorkflowExecutionLogContext(String workflowExecutionId, OverrideBehavior behavior) {
    super(ID, workflowExecutionId, behavior);
  }
}
