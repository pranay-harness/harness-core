package io.harness.redesign.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.Interrupt;
import io.harness.plan.Plan;

import java.io.IOException;
import java.io.OutputStream;

@OwnedBy(CDC)
public interface CustomExecutionService {
  PlanExecution executeHttpSwitch();

  PlanExecution executeHttpFork();

  PlanExecution executeSectionPlan();

  PlanExecution executeRetryIgnorePlan();

  PlanExecution executeRetryAbortPlan();

  PlanExecution executeInterventionPlan();

  PlanExecution executeRollbackPlan();

  PlanExecution executeSimpleShellScriptPlan(String accountId, String appId);

  PlanExecution executeSimpleTimeoutPlan(String accountId, String appId);

  PlanExecution executeTaskChainPlanV1();

  PlanExecution executeSectionChainPlan();

  PlanExecution executeSectionChainPlanWithFailure();

  PlanExecution executeSectionChainPlanWithNoChildren();

  PlanExecution executeSectionChainRollbackPlan();

  PlanExecution testGraphPlan();

  OrchestrationGraphDTO getOrchestrationGraph(String executionPlanId);

  OrchestrationGraphDTO getOrchestrationGraphV2(String executionPlanId);

  OrchestrationGraphDTO getPartialOrchestrationGraph(String startingSetupNodeId, String executionPlanId);

  OrchestrationGraphDTO getPartialOrchestrationGraphFromIdentifier(String identifier, String executionPlanId);

  void getGraphVisualization(String executionPlanId, OutputStream output) throws IOException;

  PlanExecution executeSingleBarrierPlan();

  PlanExecution executeMultipleBarriersPlan();

  PlanExecution executeResourceRestraintPlan();

  PlanExecution executeResourceRestraintPlanForFunctionalTest(Plan plan, EmbeddedUser embeddedUser);

  PlanExecution executeResourceRestraintWithWaitPlan();

  PlanExecution executeSkipChildren();

  PlanExecution executeSkipNode();

  Interrupt registerInterrupt(InterruptPackage interruptPackage);
}
