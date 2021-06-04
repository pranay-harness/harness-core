package io.harness.engine;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.algorithm.HashGenerator;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.engine.observers.OrchestrationStartObserver;
import io.harness.engine.observers.beans.OrchestrationStartInfo;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.interrupts.Interrupt;
import io.harness.observer.Subject;
import io.harness.plan.Plan;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.events.OrchestrationEvent;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class OrchestrationServiceImpl implements OrchestrationService {
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private InterruptManager interruptManager;
  @Inject private PlanExecutionMetadataService planExecutionMetadataService;

  @Getter private final Subject<OrchestrationStartObserver> orchestrationStartSubject = new Subject<>();

  @Override
  public PlanExecution startExecution(Plan plan, ExecutionMetadata metadata) {
    return startExecution(plan, new HashMap<>(), metadata, PlanExecutionMetadata.builder().build());
  }

  @Override
  public PlanExecution startExecution(@Valid Plan plan, Map<String, String> setupAbstractions,
      ExecutionMetadata metadata, PlanExecutionMetadata planExecutionMetadata) {
    PlanExecution planExecution = PlanExecution.builder()
                                      .uuid(metadata.getExecutionUuid())
                                      .plan(plan)
                                      .setupAbstractions(setupAbstractions)
                                      .status(Status.RUNNING)
                                      .startTs(System.currentTimeMillis())
                                      .metadata(metadata)
                                      .build();
    PlanNodeProto planNode = plan.fetchStartingNode();
    if (planNode == null) {
      log.error("Cannot Start Execution for empty plan");
      return null;
    }

    PlanExecution savedPlanExecution = planExecutionService.save(planExecution);

    planExecutionMetadataService.save(planExecutionMetadata);

    Ambiance ambiance = Ambiance.newBuilder()
                            .putAllSetupAbstractions(setupAbstractions)
                            .setPlanExecutionId(savedPlanExecution.getUuid())
                            .setMetadata(metadata)
                            .setExpressionFunctorToken(HashGenerator.generateIntegerHash())
                            .build();
    eventEmitter.emitEvent(
        OrchestrationEvent.builder().ambiance(ambiance).eventType(OrchestrationEventType.ORCHESTRATION_START).build());
    orchestrationStartSubject.fireInform(OrchestrationStartObxxxxxxxx:onStart,
        OrchestrationStartInfo.builder().ambiance(ambiance).planExecutionMetadata(planExecutionMetadata).build());
    orchestrationEngine.triggerExecution(ambiance, planNode);
    return savedPlanExecution;
  }

  @Override
  public Interrupt registerInterrupt(InterruptPackage interruptPackage) {
    return interruptManager.register(interruptPackage);
  }
}
