package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class WorkflowLogContext extends AutoLogContext {
  public WorkflowLogContext(String workflowId) {
    super("workflowId", workflowId);
  }
}
