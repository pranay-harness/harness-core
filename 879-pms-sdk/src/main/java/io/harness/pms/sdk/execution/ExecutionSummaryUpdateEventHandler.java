package io.harness.pms.sdk.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.service.ExecutionSummaryUpdateRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.PmsSdkModuleUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.core.execution.PmsExecutionGrpcClient;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.grpc.StatusRuntimeException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class ExecutionSummaryUpdateEventHandler implements OrchestrationEventHandler {
  @Inject(optional = true) PmsExecutionGrpcClient pmsClient;
  @Inject(optional = true) ExecutionSummaryModuleInfoProvider executionSummaryModuleInfoProvider;
  @Inject @Named(PmsSdkModuleUtils.SDK_SERVICE_NAME) String serviceName;

  public ExecutionSummaryUpdateEventHandler() {}

  // Yes
  @Override
  public void handleEvent(OrchestrationEvent orchestrationEvent) {
    if (orchestrationEvent.getEventType() == OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE) {
      log.info("Starting ExecutionSummaryUpdateEvent handler orchestration event of type [{}] for status [{}]",
          orchestrationEvent.getEventType(), orchestrationEvent.getStatus());
    }
    if (executionSummaryModuleInfoProvider == null
        || !executionSummaryModuleInfoProvider.shouldRun(orchestrationEvent)) {
      log.info("Ignoring ExecutionSummaryUpdate handler because the module info won't update for this step");
      return;
    }
    Ambiance ambiance = orchestrationEvent.getAmbiance();
    Level level = AmbianceUtils.obtainCurrentLevel(ambiance);
    ExecutionSummaryUpdateRequest.Builder executionSummaryUpdateRequest =
        ExecutionSummaryUpdateRequest.newBuilder()
            .setModuleName(serviceName)
            .setPlanExecutionId(ambiance.getPlanExecutionId())
            .setNodeExecutionId(AmbianceUtils.obtainCurrentLevel(ambiance).getRuntimeId());
    if (ambiance.getLevelsCount() >= 3) {
      if (Objects.equals(ambiance.getLevels(2).getGroup(), "STAGE")) {
        executionSummaryUpdateRequest.setNodeUuid(ambiance.getLevels(2).getSetupId());
      } else if (ambiance.getLevelsCount() >= 4) {
        executionSummaryUpdateRequest.setNodeUuid(ambiance.getLevels(3).getSetupId());
      }
    }
    if (Objects.equals(level.getGroup(), "STAGE")) {
      executionSummaryUpdateRequest.setNodeUuid(level.getSetupId());
    }
    String pipelineInfoJson = RecastOrchestrationUtils.toJson(
        executionSummaryModuleInfoProvider.getPipelineLevelModuleInfo(orchestrationEvent));
    if (EmptyPredicate.isNotEmpty(pipelineInfoJson)) {
      executionSummaryUpdateRequest.setPipelineModuleInfoJson(pipelineInfoJson);
    }
    String stageInfoJson =
        RecastOrchestrationUtils.toJson(executionSummaryModuleInfoProvider.getStageLevelModuleInfo(orchestrationEvent));
    if (EmptyPredicate.isNotEmpty(stageInfoJson)) {
      executionSummaryUpdateRequest.setNodeModuleInfoJson(stageInfoJson);
    }
    try {
      pmsClient.updateExecutionSummary(executionSummaryUpdateRequest.build());
    } catch (StatusRuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw ex;
    }
    log.info("Completed ExecutionSummaryUpdateEvent handler orchestration event of type [{}] for nodeExecutionId [{}]",
        orchestrationEvent.getEventType(), orchestrationEvent.getStatus());
  }
}
