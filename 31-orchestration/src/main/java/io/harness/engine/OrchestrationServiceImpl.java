package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.execution.status.Status;
import io.harness.interrupts.Interrupt;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import javax.validation.Valid;

@Redesign
@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class OrchestrationServiceImpl implements OrchestrationService {
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private InterruptManager interruptManager;

  @Override
  public PlanExecution startExecution(Plan plan, EmbeddedUser createdBy) {
    return startExecution(plan, null, createdBy);
  }

  @Override
  public PlanExecution startExecution(@Valid Plan plan, Map<String, String> setupAbstractions, EmbeddedUser createdBy) {
    PlanExecution planExecution = PlanExecution.builder()
                                      .uuid(generateUuid())
                                      .plan(plan)
                                      .setupAbstractions(setupAbstractions)
                                      .status(Status.RUNNING)
                                      .createdBy(createdBy)
                                      .startTs(System.currentTimeMillis())
                                      .build();
    PlanNode planNode = plan.fetchStartingNode();
    if (planNode == null) {
      logger.error("Cannot Start Execution for empty plan");
      return null;
    }
    PlanExecution savedPlanExecution = planExecutionService.save(planExecution);
    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .ambiance(Ambiance.builder()
                                             .planExecutionId(savedPlanExecution.getUuid())
                                             .setupAbstractions(savedPlanExecution.getSetupAbstractions())
                                             .build())
                               .eventType(OrchestrationEventType.ORCHESTRATION_START)
                               .build());
    Ambiance ambiance =
        Ambiance.builder().setupAbstractions(setupAbstractions).planExecutionId(savedPlanExecution.getUuid()).build();
    orchestrationEngine.triggerExecution(ambiance, planNode);
    return savedPlanExecution;
  }

  @Override
  public Interrupt registerInterrupt(InterruptPackage interruptPackage) {
    return interruptManager.register(interruptPackage);
  }
}
