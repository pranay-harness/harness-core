package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class WorkflowExecutionLogContext extends AutoLogContext {
  public static final String ID = "workflowExecutionId";

  public WorkflowExecutionLogContext(String workflowExecutionId) {
    super(ID, workflowExecutionId);
  }
}
