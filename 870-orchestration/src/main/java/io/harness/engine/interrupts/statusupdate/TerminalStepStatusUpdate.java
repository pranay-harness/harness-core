package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.utils.OrchestrationUtils;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(PIPELINE)
public class TerminalStepStatusUpdate implements StepStatusUpdate {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    List<NodeExecution> nodeExecutions = nodeExecutionService.fetchNodeExecutionsWithoutOldRetriesAndStatusIn(
        stepStatusUpdateInfo.getPlanExecutionId(), StatusUtils.activeStatuses());
    List<NodeExecution> filteredExecutions =
        nodeExecutions.stream()
            .filter(ne -> !ne.getUuid().equals(stepStatusUpdateInfo.getNodeExecutionId()))
            .collect(Collectors.toList());
    Status planStatus =
        OrchestrationUtils.calculateStatus(filteredExecutions, stepStatusUpdateInfo.getPlanExecutionId());
    if (!StatusUtils.isFinalStatus(planStatus)) {
      planExecutionService.updateStatus(stepStatusUpdateInfo.getPlanExecutionId(), planStatus);
    }
  }
}
