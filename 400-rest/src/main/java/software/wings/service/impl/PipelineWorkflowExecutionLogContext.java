package software.wings.service.impl;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;

import software.wings.beans.PipelineExecution;

public class PipelineWorkflowExecutionLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(PipelineExecution.class);

  public PipelineWorkflowExecutionLogContext(String workflowExecutionId, OverrideBehavior behavior) {
    super(ID, workflowExecutionId, behavior);
  }
}
