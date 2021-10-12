package io.harness.engine.pms.execution.strategy.plan;

import static io.harness.pms.contracts.execution.Status.ERRORED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.GovernanceService;
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
import io.harness.opaclient.model.OpaConstants;
import io.harness.plan.Node;
import io.harness.plan.Plan;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.governance.GovernanceMetadata;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.springdata.TransactionHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PlanExecutionStrategy implements NodeExecutionStrategy<Plan, PlanExecution, PlanExecutionMetadata> {
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private PlanExecutionMetadataService planExecutionMetadataService;
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private TransactionHelper transactionHelper;
  @Inject private GovernanceService governanceService;

  @Getter private final Subject<OrchestrationStartObserver> orchestrationStartSubject = new Subject<>();
  @Getter private final Subject<OrchestrationEndObserver> orchestrationEndSubject = new Subject<>();

  @Override
  public PlanExecution triggerNode(Ambiance ambiance, Plan plan, PlanExecutionMetadata metadata) {
    GovernanceMetadata governanceMetadata = governanceService.evaluateGovernancePolicies(metadata.getYaml(),
        ambiance.getSetupAbstractionsMap().get(SetupAbstractionKeys.accountId),
        OpaConstants.OPA_EVALUATION_ACTION_PIPELINE_RUN);
    PlanExecution planExecution = createPlanExecution(ambiance, metadata, governanceMetadata);
    eventEmitter.emitEvent(
        OrchestrationEvent.newBuilder()
            .setAmbiance(ambiance)
            .setEventType(OrchestrationEventType.ORCHESTRATION_START)
            .setTriggerPayload(metadata.getTriggerPayload() != null ? metadata.getTriggerPayload()
                                                                    : TriggerPayload.newBuilder().build())
            .build());

    Node planNode = plan.fetchStartingPlanNode();
    if (planNode == null) {
      throw new InvalidRequestException("Starting node for plan cannot be null");
    }

    orchestrationStartSubject.fireInform(OrchestrationStartObxxxxxxxx:onStart,
        OrchestrationStartInfo.builder().ambiance(ambiance).planExecutionMetadata(metadata).build());

    if (governanceMetadata.getDeny()) {
      return planExecutionService.updateStatus(
          ambiance.getPlanExecutionId(), ERRORED, ops -> ops.set(PlanExecutionKeys.endTs, System.currentTimeMillis()));
    } else {
      executorService.submit(() -> orchestrationEngine.triggerNode(ambiance, planNode, null));
      return planExecution;
    }
  }

  private PlanExecution createPlanExecution(
      Ambiance ambiance, PlanExecutionMetadata planExecutionMetadata, GovernanceMetadata governanceMetadata) {
    PlanExecution planExecution = PlanExecution.builder()
                                      .uuid(ambiance.getPlanExecutionId())
                                      .planId(ambiance.getPlanId())
                                      .setupAbstractions(ambiance.getSetupAbstractionsMap())
                                      .status(Status.RUNNING)
                                      .startTs(System.currentTimeMillis())
                                      .governanceMetadata(governanceMetadata)
                                      .metadata(ambiance.getMetadata())
                                      .build();

    return transactionHelper.performTransaction(() -> {
      planExecutionMetadataService.save(planExecutionMetadata);
      return planExecutionService.save(planExecution);
    });
  }

  @Override
  public void endNodeExecution(Ambiance ambiance) {
    Status status = planExecutionService.calculateStatus(ambiance.getPlanExecutionId());
    PlanExecution planExecution = planExecutionService.updateStatus(
        ambiance.getPlanExecutionId(), status, ops -> ops.set(PlanExecutionKeys.endTs, System.currentTimeMillis()));
    if (planExecution == null) {
      log.error("Cannot transition plan execution to status : {}", status);
      // TODO: Incorporate error handling
      planExecution = planExecutionService.updateStatus(
          ambiance.getPlanExecutionId(), ERRORED, ops -> ops.set(PlanExecutionKeys.endTs, System.currentTimeMillis()));
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
