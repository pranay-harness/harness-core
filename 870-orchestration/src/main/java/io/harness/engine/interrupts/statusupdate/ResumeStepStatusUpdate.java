package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.contracts.execution.Status.PAUSED;
import static io.harness.pms.contracts.execution.Status.RUNNING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.Status;

import com.google.inject.Inject;
import java.util.EnumSet;

@OwnedBy(PIPELINE)
public class ResumeStepStatusUpdate implements StepStatusUpdate {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    NodeExecution nodeExecution = nodeExecutionService.get(stepStatusUpdateInfo.getNodeExecutionId());
    if (nodeExecution.getParentId() == null) {
      planExecutionService.updateCalculatedStatus(stepStatusUpdateInfo.getPlanExecutionId());
      return;
    }
    NodeExecution parent = nodeExecutionService.get(nodeExecution.getParentId());
    if (parent.getStatus() != PAUSED) {
      return;
    }
    nodeExecutionService.updateStatusWithOps(nodeExecution.getParentId(), RUNNING, null, EnumSet.noneOf(Status.class));
  }
}