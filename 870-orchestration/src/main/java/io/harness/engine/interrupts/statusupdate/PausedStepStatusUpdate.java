package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.contracts.execution.Status.PAUSED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.interrupts.InterruptService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.execution.utils.StatusUtils;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(CDC)
public class PausedStepStatusUpdate implements StepStatusUpdate {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private InterruptService interruptService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    boolean pausePlan = pauseParents(stepStatusUpdateInfo.getNodeExecutionId(), stepStatusUpdateInfo.getInterruptId());
    if (pausePlan) {
      PlanExecution planExecution = planExecutionService.get(stepStatusUpdateInfo.getPlanExecutionId());
      planExecutionService.updateStatus(planExecution.getUuid(), PAUSED);
    }
  }

  private boolean pauseParents(String nodeExecutionId, String interruptId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (nodeExecution.getParentId() == null) {
      return true;
    }
    List<NodeExecution> flowingChildren = nodeExecutionService.findByParentIdAndStatusIn(
        nodeExecution.getParentId(), StatusUtils.unpausableChildStatuses());
    if (isEmpty(flowingChildren)) {
      Interrupt interrupt = interruptService.get(interruptId);
      // Update Status
      nodeExecutionService.updateStatusWithOps(nodeExecution.getParentId(), PAUSED,
          ops
          -> ops.addToSet(NodeExecutionKeys.interruptHistories,
              InterruptEffect.builder()
                  .interruptId(interruptId)
                  .tookEffectAt(System.currentTimeMillis())
                  .interruptType(InterruptType.PAUSE_ALL)
                  .interruptConfig(interrupt.getInterruptConfig())
                  .build()));
      return pauseParents(nodeExecution.getParentId(), interruptId);
    } else {
      return false;
    }
  }
}
