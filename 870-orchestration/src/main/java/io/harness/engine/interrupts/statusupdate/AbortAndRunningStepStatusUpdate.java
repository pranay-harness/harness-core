package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.contracts.execution.Status.RUNNING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.Status;

import com.google.inject.Inject;
import java.util.EnumSet;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
public class AbortAndRunningStepStatusUpdate implements StepStatusUpdate {
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    NodeExecution nodeExecution = nodeExecutionService.get(stepStatusUpdateInfo.getNodeExecutionId());
    if (nodeExecution.getParentId() == null) {
      return;
    }
    NodeExecution updatedParent = nodeExecutionService.updateStatusWithOps(
        nodeExecution.getParentId(), RUNNING, null, EnumSet.noneOf(Status.class));
    if (updatedParent == null) {
      log.warn("Cannot mark parent of Aborted node to running parentId: {}, nodeExecutionId: {}",
          nodeExecution.getParentId(), nodeExecution.getUuid());
    }
  }
}