package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.contracts.execution.Status.PAUSED;
import static io.harness.pms.contracts.execution.Status.RUNNING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.interrupts.InterruptService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.interrupts.InterruptType;

import com.google.inject.Inject;

@OwnedBy(CDC)
public class ResumeStepStatusUpdate implements StepStatusUpdate {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private InterruptService interruptService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    boolean resumePlan =
        resumeParents(stepStatusUpdateInfo.getNodeExecutionId(), stepStatusUpdateInfo.getInterruptId());
    if (resumePlan) {
      PlanExecution planExecution = planExecutionService.get(stepStatusUpdateInfo.getPlanExecutionId());
      planExecutionService.updateStatus(planExecution.getUuid(), RUNNING);
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
    Interrupt interrupt = interruptService.get(interruptId);
    nodeExecutionService.updateStatusWithOps(nodeExecution.getParentId(), RUNNING,
        ops
        -> ops.addToSet(NodeExecutionKeys.interruptHistories,
            InterruptEffect.builder()
                .interruptId(interruptId)
                .tookEffectAt(System.currentTimeMillis())
                .interruptType(InterruptType.RESUME_ALL)
                .interruptConfig(interrupt.getInterruptConfig())
                .build()));
    return resumeParents(nodeExecution.getParentId(), interruptId);
  }
}
