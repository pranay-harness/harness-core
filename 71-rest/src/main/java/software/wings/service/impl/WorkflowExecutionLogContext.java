package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class WorkflowExecutionLogContext extends AutoLogContext {
  public WorkflowExecutionLogContext(String workflowExecutionId) {
    super("workflowExecutionId", workflowExecutionId);
  }
}
