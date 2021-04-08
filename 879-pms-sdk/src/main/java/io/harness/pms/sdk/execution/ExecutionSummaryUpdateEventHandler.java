package io.harness.pms.sdk.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.service.ExecutionSummaryUpdateRequest;
import io.harness.pms.sdk.PmsSdkModuleUtils;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
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
public class ExecutionSummaryUpdateEventHandler implements AsyncOrchestrationEventHandler {
  @Inject(optional = true) PmsExecutionGrpcClient pmsClient;
  @Inject(optional = true) ExecutionSummaryModuleInfoProvider executionSummaryModuleInfoProvider;
  @Inject @Named(PmsSdkModuleUtils.SDK_SERVICE_NAME) String serviceName;

  public ExecutionSummaryUpdateEventHandler() {}

  @Override
  public void handleEvent(OrchestrationEvent orchestrationEvent) {
    if (orchestrationEvent.getNodeExecutionProto() != null) {
      log.info("Starting ExecutionSummaryUpdateEvent handler orchestration event of type [{}] for nodeExecutionId [{}]",
          orchestrationEvent.getEventType(), orchestrationEvent.getNodeExecutionProto().getStatus());
    }
    NodeExecutionProto nodeExecutionProto = orchestrationEvent.getNodeExecutionProto();
    ExecutionSummaryUpdateRequest.Builder executionSummaryUpdateRequest =
        ExecutionSummaryUpdateRequest.newBuilder()
            .setModuleName(serviceName)
            .setPlanExecutionId(nodeExecutionProto.getAmbiance().getPlanExecutionId())
            .setNodeExecutionId(nodeExecutionProto.getUuid());
    if (nodeExecutionProto.getAmbiance().getLevelsCount() >= 3) {
      if (Objects.equals(nodeExecutionProto.getAmbiance().getLevels(2).getGroup(), "STAGE")) {
        executionSummaryUpdateRequest.setNodeUuid(nodeExecutionProto.getAmbiance().getLevels(2).getSetupId());
      } else if (nodeExecutionProto.getAmbiance().getLevelsCount() >= 4) {
        executionSummaryUpdateRequest.setNodeUuid(nodeExecutionProto.getAmbiance().getLevels(3).getSetupId());
      }
    }
    if (Objects.equals(nodeExecutionProto.getNode().getGroup(), "STAGE")) {
      executionSummaryUpdateRequest.setNodeUuid(nodeExecutionProto.getNode().getUuid());
    }
    String pipelineInfoJson = RecastOrchestrationUtils.toDocumentJson(
        executionSummaryModuleInfoProvider.getPipelineLevelModuleInfo(nodeExecutionProto));
    if (EmptyPredicate.isNotEmpty(pipelineInfoJson)) {
      executionSummaryUpdateRequest.setPipelineModuleInfoJson(pipelineInfoJson);
    }
    String stageInfoJson = RecastOrchestrationUtils.toDocumentJson(
        executionSummaryModuleInfoProvider.getStageLevelModuleInfo(nodeExecutionProto));
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
        orchestrationEvent.getEventType(), orchestrationEvent.getNodeExecutionProto().getStatus());
  }
}
