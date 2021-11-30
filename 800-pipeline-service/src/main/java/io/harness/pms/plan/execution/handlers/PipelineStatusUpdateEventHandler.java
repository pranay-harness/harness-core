package io.harness.pms.plan.execution.handlers;

import static io.harness.data.structure.HarnessStringUtils.emptyIfNull;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.observers.OrchestrationEndObserver;
import io.harness.engine.observers.PlanStatusUpdateObserver;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.pipeline.observer.OrchestrationObserverUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.repositories.executions.AccountExecutionMetadataRepository;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Optional;
import java.util.Set;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineStatusUpdateEventHandler implements PlanStatusUpdateObserver, OrchestrationEndObserver {
  private static final String PRIVATE_REPO_BUILD_CI = "ci_private_build";
  private final PlanExecutionService planExecutionService;
  private final PmsExecutionSummaryRespository pmsExecutionSummaryRepository;
  private final AccountExecutionMetadataRepository accountExecutionMetadataRepository;
  private OrchestrationEventEmitter eventEmitter;

  @Inject
  public PipelineStatusUpdateEventHandler(PlanExecutionService planExecutionService,
      PmsExecutionSummaryRespository pmsExecutionSummaryRepository,
      AccountExecutionMetadataRepository accountExecutionMetadataRepository, OrchestrationEventEmitter eventEmitter) {
    this.planExecutionService = planExecutionService;
    this.pmsExecutionSummaryRepository = pmsExecutionSummaryRepository;
    this.accountExecutionMetadataRepository = accountExecutionMetadataRepository;
    this.eventEmitter = eventEmitter;
  }

  @Override
  public void onPlanStatusUpdate(Ambiance ambiance) {
    String planExecutionId = ambiance.getPlanExecutionId();
    PlanExecution planExecution = planExecutionService.get(planExecutionId);

    ExecutionStatus status = ExecutionStatus.getExecutionStatus(planExecution.getStatus());

    Update update = new Update();

    update.set(PlanExecutionSummaryKeys.internalStatus, planExecution.getStatus());
    update.set(PlanExecutionSummaryKeys.status, status);
    if (StatusUtils.isFinalStatus(status.getEngineStatus())) {
      update.set(PlanExecutionSummaryKeys.endTs, planExecution.getEndTs());
    }

    Criteria criteria = Criteria.where(PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }

  @Override
  public void onEnd(Ambiance ambiance) {
    Optional<PipelineExecutionSummaryEntity> pipelineExecutionSummaryEntity =
        pmsExecutionSummaryRepository
            .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionIdAndPipelineDeletedNot(
                AmbianceUtils.getAccountId(ambiance), AmbianceUtils.getOrgIdentifier(ambiance),
                AmbianceUtils.getProjectIdentifier(ambiance), ambiance.getPlanExecutionId(), true);
    if (pipelineExecutionSummaryEntity.isPresent()) {
      Set<String> executedModules =
          OrchestrationObserverUtils.getExecutedModulesInPipeline(pipelineExecutionSummaryEntity.get());
      Update update = new Update();
      update.set(PlanExecutionSummaryKeys.executedModules, executedModules);
      Criteria criteria = Criteria.where(PlanExecutionSummaryKeys.planExecutionId).is(ambiance.getPlanExecutionId());
      Query query = new Query(criteria);
      PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity1 =
          pmsExecutionSummaryRepository.update(query, update);
      if (executedModules.contains(ModuleType.CI.name().toLowerCase())
          && pipelineExecutionSummaryEntity.get().getModuleInfo() != null
          && pipelineExecutionSummaryEntity.get().getModuleInfo().get("ci") != null) {
        if (pipelineExecutionSummaryEntity.get().getModuleInfo().get("ci").getBoolean("isPrivateRepo", false)) {
          executedModules.add(PRIVATE_REPO_BUILD_CI);
        }
      }
      accountExecutionMetadataRepository.updateAccountExecutionMetadata(
          AmbianceUtils.getAccountId(ambiance), executedModules, pipelineExecutionSummaryEntity1.getStartTs());
      for (String module : executedModules) {
        eventEmitter.emitEvent(buildEndEvent(ambiance, module,
            pipelineExecutionSummaryEntity1.getStatus().getEngineStatus(),
            pipelineExecutionSummaryEntity1.getModuleInfo().get(module), pipelineExecutionSummaryEntity1.getEndTs()));
      }
    }
  }

  private OrchestrationEvent buildEndEvent(
      Ambiance ambiance, String module, Status status, Document moduleInfo, long endTs) {
    return OrchestrationEvent.newBuilder()
        .setAmbiance(ambiance)
        .setServiceName(module)
        .setEventType(OrchestrationEventType.ORCHESTRATION_END)
        .setModuleInfo(ByteString.copyFromUtf8(emptyIfNull(RecastOrchestrationUtils.toJson(moduleInfo))))
        .setStatus(status)
        .setEndTs(endTs)
        .build();
  }
}
