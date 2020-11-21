package io.harness.engine.status;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.execution.Status.PAUSED;
import static io.harness.pms.execution.Status.RUNNING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.InterruptEffect;

import com.google.inject.Inject;

@OwnedBy(CDC)
public class ResumeStepStatusUpdate implements StepStatusUpdate {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    boolean resumePlan =
        resumeParents(stepStatusUpdateInfo.getNodeExecutionId(), stepStatusUpdateInfo.getInterruptId());
    if (resumePlan) {
      planExecutionService.updateStatus(stepStatusUpdateInfo.getPlanExecutionId(), RUNNING);
    }
  }

  private boolean resumeParents(String nodeExecutionId, String interruptId) {
    // Update Status
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (nodeExecution.getParentId() == null) {
      return true;
    }
    NodeExecution parent = nodeExecutionService.get(nodeExecution.getParentId());
    if (parent.getStatus() != PAUSED) {
      return true;
    }

    nodeExecutionService.updateStatusWithOps(nodeExecution.getParentId(), RUNNING,
        ops
        -> ops.addToSet(NodeExecutionKeys.interruptHistories,
            InterruptEffect.builder()
                .interruptId(interruptId)
                .tookEffectAt(System.currentTimeMillis())
                .interruptType(ExecutionInterruptType.RESUME_ALL)
                .build()));
    return resumeParents(nodeExecution.getParentId(), interruptId);
  }
}
