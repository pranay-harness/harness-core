package io.harness.redesign.services;

import io.harness.annotations.Redesign;
import io.harness.execution.PlanExecution;

@Redesign
public interface CustomExecutionService {
  PlanExecution executeHttpSwitch();

  PlanExecution executeHttpFork();

  PlanExecution executeSectionPlan();

  PlanExecution executeRetryPlan();
}
