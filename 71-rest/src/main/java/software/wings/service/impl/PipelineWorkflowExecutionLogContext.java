package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class PipelineWorkflowExecutionLogContext extends AutoLogContext {
  public static final String ID = "pipelineExecutionId";

  public PipelineWorkflowExecutionLogContext(String workflowExecutionId, OverrideBehavior behavior) {
    super(ID, workflowExecutionId, behavior);
  }
}
