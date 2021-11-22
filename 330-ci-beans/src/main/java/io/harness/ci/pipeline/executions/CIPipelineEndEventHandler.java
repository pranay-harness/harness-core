package io.harness.ci.pipeline.executions;

import io.harness.ModuleType;
import io.harness.ci.repositories.CIAccountExecutionMetadataRepository;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.pipeline.observer.OrchestrationObserverUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.Set;

@Singleton
public class CIPipelineEndEventHandler implements OrchestrationEventHandler {
  @Inject CIAccountExecutionMetadataRepository ciAccountExecutionMetadataRepository;
  @Inject PmsExecutionSummaryRespository pmsExecutionSummaryRepository;
  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    Optional<PipelineExecutionSummaryEntity> pipelineExecutionSummaryEntity =
        pmsExecutionSummaryRepository
            .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionIdAndPipelineDeletedNot(
                AmbianceUtils.getAccountId(ambiance), AmbianceUtils.getOrgIdentifier(ambiance),
                AmbianceUtils.getProjectIdentifier(ambiance), ambiance.getPlanExecutionId(), true);
    if (pipelineExecutionSummaryEntity.isPresent()) {
      Set<String> executedModules =
          OrchestrationObserverUtils.getExecutedModulesInPipeline(pipelineExecutionSummaryEntity.get());
      if (executedModules.contains(ModuleType.CI.name().toLowerCase())
          && pipelineExecutionSummaryEntity.get().getModuleInfo() != null
          && pipelineExecutionSummaryEntity.get().getModuleInfo().get("ci") != null) {
        if (pipelineExecutionSummaryEntity.get().getModuleInfo().get("ci").getBoolean("isPrivateRepo", false)) {
          ciAccountExecutionMetadataRepository.updateAccountExecutionMetadata(
              AmbianceUtils.getAccountId(event.getAmbiance()), event.getEndTs());
        }
      }
    }
  }
}
