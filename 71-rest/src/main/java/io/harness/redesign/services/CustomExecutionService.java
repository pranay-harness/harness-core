package io.harness.redesign.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.Interrupt;

@OwnedBy(CDC)
@Redesign
public interface CustomExecutionService {
  PlanExecution executeHttpSwitch();

  PlanExecution executeHttpFork();

  PlanExecution executeSectionPlan();

  PlanExecution executeRetryPlan();

  PlanExecution executeRollbackPlan();

  PlanExecution executeSimpleShellScriptPlan();

  // Interrupts

  Interrupt registerInterrupt(String planExecutionId);
}
