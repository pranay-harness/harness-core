package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.algorithm.HashGenerator;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.executions.plan.PlanService;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.engine.observers.OrchestrationStartObserver;
import io.harness.engine.observers.beans.OrchestrationStartInfo;
import io.harness.engine.utils.TransactionUtils;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.interrupts.Interrupt;
import io.harness.observer.Subject;
import io.harness.plan.Plan;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.serializer.ProtoUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.concurrent.ExecutorService;
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
  @Inject private PlanService planService;
  @Inject private PlanExecutionMetadataService planExecutionMetadataService;
  @Inject private TransactionUtils transactionUtils;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Getter private final Subject<OrchestrationStartObserver> orchestrationStartSubject = new Subject<>();

  @Override
  public PlanExecution startExecution(@Valid Plan plan, Map<String, String> setupAbstractions,
      ExecutionMetadata metadata, PlanExecutionMetadata planExecutionMetadata) {
    Plan savedPlan = planService.save(plan);
    return executePlan(savedPlan, setupAbstractions, metadata, planExecutionMetadata);
  }

  private PlanExecution executePlan(@Valid Plan plan, Map<String, String> setupAbstractions, ExecutionMetadata metadata,
      PlanExecutionMetadata planExecutionMetadata) {
    PlanExecution savedPlanExecution = createPlanExecution(plan, setupAbstractions, metadata, planExecutionMetadata);

    Ambiance ambiance = Ambiance.newBuilder()
                            .putAllSetupAbstractions(setupAbstractions)
                            .setPlanExecutionId(savedPlanExecution.getUuid())
                            .setPlanId(plan.getUuid())
                            .setMetadata(metadata)
                            .setExpressionFunctorToken(HashGenerator.generateIntegerHash())
                            .build();
    eventEmitter.emitEvent(OrchestrationEvent.newBuilder()
                               .setAmbiance(ambiance)
                               .setEventType(OrchestrationEventType.ORCHESTRATION_START)
                               .setCreatedAt(ProtoUtils.unixMillisToTimestamp(System.currentTimeMillis()))
                               .setTriggerPayload(planExecutionMetadata.getTriggerPayload() != null
                                       ? planExecutionMetadata.getTriggerPayload()
                                       : TriggerPayload.newBuilder().build())
                               .build());
    orchestrationStartSubject.fireInform(OrchestrationStartObxxxxxxxx:onStart,
        OrchestrationStartInfo.builder().ambiance(ambiance).planExecutionMetadata(planExecutionMetadata).build());

    PlanNodeProto planNode = plan.fetchStartingNode();
    if (planNode == null) {
      log.error("Cannot Start Execution for empty plan");
      return null;
    }
    submitToEngine(ambiance, planNode);
    return savedPlanExecution;
  }

  @VisibleForTesting
  void submitToEngine(Ambiance ambiance, PlanNodeProto planNode) {
    executorService.submit(() -> orchestrationEngine.triggerExecution(ambiance, planNode));
  }

  private PlanExecution createPlanExecution(@Valid Plan plan, Map<String, String> setupAbstractions,
      ExecutionMetadata metadata, PlanExecutionMetadata planExecutionMetadata) {
    PlanExecution planExecution = PlanExecution.builder()
                                      .uuid(generateUuid())
                                      .planId(plan.getUuid())
                                      .setupAbstractions(setupAbstractions)
                                      .status(Status.RUNNING)
                                      .startTs(System.currentTimeMillis())
                                      .metadata(metadata)
                                      .build();

    return transactionUtils.performTransaction(() -> {
      PlanExecution execution = planExecutionService.save(planExecution);
      planExecutionMetadataService.save(planExecutionMetadata.withPlanExecutionId(execution.getUuid()));
      return planExecution;
    });
  }

  @Override
  public Interrupt registerInterrupt(InterruptPackage interruptPackage) {
    return interruptManager.register(interruptPackage);
  }
}
