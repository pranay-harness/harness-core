package io.harness.redesign.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.Interrupt;
import io.harness.presentation.Graph;

@OwnedBy(CDC)
@Redesign
public interface CustomExecutionService {
  PlanExecution executeHttpSwitch();

  PlanExecution executeHttpFork();

  PlanExecution executeSectionPlan();

  PlanExecution executeRetryPlan();

  PlanExecution executeRollbackPlan();

  PlanExecution executeSimpleShellScriptPlan(String accountId, String appId);

  PlanExecution executeTaskChainPlan();

  PlanExecution executeSectionChainPlan();

  PlanExecution testInfraState();

  PlanExecution testGraphPlan();

  Graph getGraph(String executionPlanId);

  // Interrupts

  Interrupt registerInterrupt(String planExecutionId);

  PlanExecution testExecutionPlanCreator(String pipelineYaml, String accountId, String appId);
}
