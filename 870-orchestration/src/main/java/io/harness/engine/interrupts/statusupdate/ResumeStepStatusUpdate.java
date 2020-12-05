package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.execution.Status.PAUSED;
import static io.harness.pms.execution.Status.RUNNING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.interrupts.InterruptHelper;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.InterruptEffect;

import com.google.inject.Inject;

@OwnedBy(CDC)
public class ResumeStepStatusUpdate implements StepStatusUpdate {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private OrchestrationEventEmitter eventEmitter;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    boolean resumePlan =
        resumeParents(stepStatusUpdateInfo.getNodeExecutionId(), stepStatusUpdateInfo.getInterruptId());
    if (resumePlan) {
      PlanExecution planExecution = planExecutionService.get(stepStatusUpdateInfo.getPlanExecutionId());
      planExecutionService.updateStatus(planExecution.getUuid(), RUNNING);
      eventEmitter.emitEvent(OrchestrationEvent.builder()
                                 .ambiance(InterruptHelper.buildFromPlanExecution(planExecution))
                                 .eventType(OrchestrationEventType.PLAN_EXECUTION_STATUS_UPDATE)
                                 .build());
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
