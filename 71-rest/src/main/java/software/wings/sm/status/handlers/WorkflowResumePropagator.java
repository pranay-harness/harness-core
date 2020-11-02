package software.wings.sm.status.handlers;

import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.WorkflowExecution;
import software.wings.service.impl.WorkflowExecutionUpdate;
import software.wings.sm.status.StateStatusUpdateInfo;
import software.wings.sm.status.WorkflowStatusPropagator;

@Slf4j
public class WorkflowResumePropagator implements WorkflowStatusPropagator {
  @Inject private WorkflowStatusPropagatorHelper propagatorHelper;
  @Inject private WorkflowExecutionUpdate workflowExecutionUpdate;

  @Override
  public void handleStatusUpdate(StateStatusUpdateInfo updateInfo) {
    WorkflowExecution updatedExecution = propagatorHelper.updateStatus(
        updateInfo.getAppId(), updateInfo.getWorkflowExecutionId(), singletonList(PAUSED), RUNNING);
    if (updatedExecution == null) {
      log.info("Updating status to paused failed for execution id: {}", updateInfo.getWorkflowExecutionId());
    } else {
      workflowExecutionUpdate.publish(updatedExecution);
    }

    WorkflowExecution execution =
        propagatorHelper.obtainExecution(updateInfo.getAppId(), updateInfo.getWorkflowExecutionId());
    if (propagatorHelper.shouldResumePipeline(updateInfo.getAppId(), execution.getPipelineExecutionId())) {
      WorkflowExecution pipelineExecution = propagatorHelper.updateStatus(
          updateInfo.getAppId(), execution.getPipelineExecutionId(), singletonList(PAUSED), RUNNING);
      if (pipelineExecution == null) {
        log.info("Updating status to paused failed for Pipeline with id: {}", execution.getPipelineExecution());
      }
    }
  }
}
