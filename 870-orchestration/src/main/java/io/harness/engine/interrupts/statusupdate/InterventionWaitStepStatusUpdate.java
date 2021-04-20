package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.contracts.execution.Status.INTERVENTION_WAITING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionService;

import com.google.inject.Inject;

@OwnedBy(CDC)
public class InterventionWaitStepStatusUpdate implements StepStatusUpdate {
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    planExecutionService.updateStatus(stepStatusUpdateInfo.getPlanExecutionId(), INTERVENTION_WAITING);
  }
}
