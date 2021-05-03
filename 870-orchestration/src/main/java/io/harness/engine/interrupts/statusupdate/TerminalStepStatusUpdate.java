package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;

import com.google.inject.Inject;

@OwnedBy(PIPELINE)
public class TerminalStepStatusUpdate implements StepStatusUpdate {
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    Status planStatus = planExecutionService.calculateStatus(stepStatusUpdateInfo.getPlanExecutionId());
    if (!StatusUtils.isFinalStatus(planStatus)) {
      planExecutionService.updateStatus(stepStatusUpdateInfo.getPlanExecutionId(), planStatus);
    }
  }
}
