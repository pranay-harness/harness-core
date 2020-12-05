package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.interrupts.Interrupt;
import io.harness.plan.Plan;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.Status;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.plan.PlanNode;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Redesign
@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class OrchestrationServiceImpl implements OrchestrationService {
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private InterruptManager interruptManager;

  @Override
  public PlanExecution startExecution(Plan plan) {
    return startExecution(plan, new HashMap<>());
  }

  @Override
  public PlanExecution startExecution(@Valid Plan plan, Map<String, String> setupAbstractions) {
    PlanExecution planExecution = PlanExecution.builder()
                                      .uuid(generateUuid())
                                      .plan(plan)
                                      .setupAbstractions(setupAbstractions)
                                      .status(Status.RUNNING)
                                      .startTs(System.currentTimeMillis())
                                      .build();
    PlanNodeProto planNode = plan.fetchStartingNode();
    if (planNode == null) {
      log.error("Cannot Start Execution for empty plan");
      return null;
    }
    PlanExecution savedPlanExecution = planExecutionService.save(planExecution);
    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .ambiance(Ambiance.newBuilder()
                                             .setPlanExecutionId(savedPlanExecution.getUuid())
                                             .putAllSetupAbstractions(savedPlanExecution.getSetupAbstractions())
                                             .build())
                               .eventType(OrchestrationEventType.ORCHESTRATION_START)
                               .build());
    Ambiance ambiance = Ambiance.newBuilder()
                            .putAllSetupAbstractions(setupAbstractions)
                            .setPlanExecutionId(savedPlanExecution.getUuid())
                            .build();
    orchestrationEngine.triggerExecution(ambiance, planNode);
    return savedPlanExecution;
  }

  @Override
  public PlanExecution rerunExecution(String planExecutionId, Map<String, String> setupAbstractions) {
    PlanExecution planExecution = planExecutionService.get(planExecutionId);
    return startExecution(planExecution.getPlan(), setupAbstractions == null ? new HashMap<>() : setupAbstractions);
  }

  @Override
  public Interrupt registerInterrupt(InterruptPackage interruptPackage) {
    return interruptManager.register(interruptPackage);
  }
}
