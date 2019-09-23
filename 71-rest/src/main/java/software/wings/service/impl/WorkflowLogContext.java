package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class WorkflowLogContext extends AutoLogContext {
  public static final String ID = "workflowId";

  public WorkflowLogContext(String workflowId) {
    super(ID, workflowId);
  }
}
