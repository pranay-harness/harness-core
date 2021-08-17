package software.wings.sm.status.handlers;

import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.beans.ExecutionStatus.RUNNING;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.WorkflowExecution;
import software.wings.service.impl.WorkflowExecutionUpdate;
import software.wings.sm.status.StateStatusUpdateInfo;
import software.wings.sm.status.WorkflowStatusPropagator;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class WorkflowPausePropagator implements WorkflowStatusPropagator {
  @Inject private WorkflowStatusPropagatorHelper propagatorHelper;
  @Inject private WorkflowExecutionUpdate workflowExecutionUpdate;

  @Override
  public void handleStatusUpdate(StateStatusUpdateInfo updateInfo) {
    WorkflowExecution updatedExecution = propagatorHelper.updateStatus(
        updateInfo.getAppId(), updateInfo.getWorkflowExecutionId(), asList(QUEUED, RUNNING), PAUSED);
    if (updatedExecution == null) {
      log.info("Updating status to paused failed for execution id: {}", updateInfo.getWorkflowExecutionId());
    } else {
      workflowExecutionUpdate.publish(updatedExecution);
    }

    WorkflowExecution execution =
        propagatorHelper.obtainExecution(updateInfo.getAppId(), updateInfo.getWorkflowExecutionId());
    if (propagatorHelper.shouldPausePipeline(updateInfo.getAppId(), execution.getPipelineExecutionId())) {
      WorkflowExecution pipelineExecution = propagatorHelper.updateStatus(
          updateInfo.getAppId(), execution.getPipelineExecutionId(), singletonList(RUNNING), PAUSED);
      if (pipelineExecution == null) {
        log.info("Updating status to paused failed for Pipeline with id: {}", execution.getPipelineExecution());
      }
    }
  }
}
