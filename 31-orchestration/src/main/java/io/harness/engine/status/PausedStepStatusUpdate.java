package io.harness.engine.status;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.execution.Status.PAUSED;

import io.harness.StatusUtils;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.InterruptEffect;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(CDC)
public class PausedStepStatusUpdate implements StepStatusUpdate {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    boolean pausePlan = pauseParents(stepStatusUpdateInfo.getNodeExecutionId(), stepStatusUpdateInfo.getInterruptId());
    if (pausePlan) {
      planExecutionService.updateStatus(stepStatusUpdateInfo.getPlanExecutionId(), PAUSED);
    }
  }

  private boolean pauseParents(String nodeExecutionId, String interruptId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (nodeExecution.getParentId() == null) {
      return true;
    }
    List<NodeExecution> flowingChildren =
        nodeExecutionService.findByParentIdAndStatusIn(nodeExecution.getParentId(), StatusUtils.flowingStatuses());
    if (isEmpty(flowingChildren)) {
      // Update Status
      nodeExecutionService.updateStatusWithOps(nodeExecution.getParentId(), PAUSED,
          ops
          -> ops.addToSet(NodeExecutionKeys.interruptHistories,
              InterruptEffect.builder()
                  .interruptId(interruptId)
                  .tookEffectAt(System.currentTimeMillis())
                  .interruptType(ExecutionInterruptType.PAUSE_ALL)
                  .build()));
      return pauseParents(nodeExecution.getParentId(), interruptId);
    } else {
      return false;
    }
  }
}
