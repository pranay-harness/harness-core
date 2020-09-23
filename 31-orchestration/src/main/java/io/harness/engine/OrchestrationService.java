package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.Interrupt;
import io.harness.plan.Plan;

import java.util.Map;
import javax.validation.Valid;

@OwnedBy(CDC)
@Redesign
public interface OrchestrationService {
  PlanExecution startExecution(@Valid Plan plan);
  PlanExecution startExecution(@Valid Plan plan, Map<String, String> setupAbstractions);
  PlanExecution rerunExecution(String planExecutionId, Map<String, String> setupAbstractions);

  Interrupt registerInterrupt(@Valid InterruptPackage interruptPackage);
}
