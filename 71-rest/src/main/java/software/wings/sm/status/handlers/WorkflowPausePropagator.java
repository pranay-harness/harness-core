package software.wings.sm.status.handlers;

import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.WorkflowExecution;
import software.wings.sm.status.StateStatusUpdateInfo;
import software.wings.sm.status.WorkflowStatusPropagator;
@Slf4j
public class WorkflowPausePropagator implements WorkflowStatusPropagator {
  @Inject private WorkflowStatusPropagatorHelper propagatorHelper;

  @Override
  public void handleStatusUpdate(StateStatusUpdateInfo updateInfo) {
    WorkflowExecution updatedExecution = propagatorHelper.updateStatus(
        updateInfo.getAppId(), updateInfo.getWorkflowExecutionId(), asList(QUEUED, RUNNING), PAUSED);
    if (updatedExecution == null) {
      logger.info("Updating status to paused failed for execution id: {}", updateInfo.getWorkflowExecutionId());
    }

    WorkflowExecution execution =
        propagatorHelper.obtainExecution(updateInfo.getAppId(), updateInfo.getWorkflowExecutionId());
    if (propagatorHelper.shouldPausePipeline(updateInfo.getAppId(), execution.getPipelineExecutionId())) {
      WorkflowExecution pipelineExecution = propagatorHelper.updateStatus(
          updateInfo.getAppId(), execution.getPipelineExecutionId(), singletonList(RUNNING), PAUSED);
      if (pipelineExecution == null) {
        logger.info("Updating status to paused failed for Pipeline with id: {}", execution.getPipelineExecution());
      }
    }
  }
}
