package io.harness.engine.pms.execution.strategy.plan;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.observers.OrchestrationEndObserver;
import io.harness.engine.observers.OrchestrationStartObserver;
import io.harness.engine.observers.beans.OrchestrationStartInfo;
import io.harness.engine.pms.execution.strategy.NodeExecutionStrategy;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.observer.Subject;
import io.harness.plan.Node;
import io.harness.plan.Plan;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PlanExecutionStrategy implements NodeExecutionStrategy<Plan> {
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private PlanExecutionMetadataService planExecutionMetadataService;
  @Inject private OrchestrationEventEmitter eventEmitter;

  @Getter private final Subject<OrchestrationStartObserver> orchestrationStartSubject = new Subject<>();
  @Getter private final Subject<OrchestrationEndObserver> orchestrationEndSubject = new Subject<>();

  @Override
  public void triggerNode(Ambiance ambiance, Plan plan) {
    Node planNode = plan.fetchStartingPlanNode();
    if (planNode == null) {
      throw new InvalidRequestException("Starting node for plan cannot be null");
    }
    Optional<PlanExecutionMetadata> metadata =
        planExecutionMetadataService.findByPlanExecutionId(ambiance.getPlanExecutionId());
    orchestrationStartSubject.fireInform(OrchestrationStartObxxxxxxxx:onStart,
        OrchestrationStartInfo.builder().ambiance(ambiance).planExecutionMetadata(metadata.get()).build());
    executorService.submit(() -> orchestrationEngine.triggerNode(ambiance, planNode));
  }

  @Override
  public void endNodeExecution(Ambiance ambiance) {
    Status status = planExecutionService.calculateStatus(ambiance.getPlanExecutionId());
    PlanExecution planExecution = planExecutionService.updateStatus(
        ambiance.getPlanExecutionId(), status, ops -> ops.set(PlanExecutionKeys.endTs, System.currentTimeMillis()));
    if (planExecution == null) {
      log.error("Cannot transition plan execution to status : {}", status);
      // TODO: Incorporate error handling
      planExecution = planExecutionService.updateStatus(ambiance.getPlanExecutionId(), Status.ERRORED,
          ops -> ops.set(PlanExecutionKeys.endTs, System.currentTimeMillis()));
    }
    eventEmitter.emitEvent(buildEndEvent(ambiance, planExecution.getStatus()));
    orchestrationEndSubject.fireInform(OrchestrationEndObxxxxxxxx:onEnd, ambiance);
  }

  private OrchestrationEvent buildEndEvent(Ambiance ambiance, Status status) {
    return OrchestrationEvent.newBuilder()
        .setAmbiance(ambiance)
        .setEventType(OrchestrationEventType.ORCHESTRATION_END)
        .setStatus(status)
        .build();
  }

  @Override
  public void handleError(Ambiance ambiance, Exception exception) {
    // TODO: Add implementation here
  }
}
