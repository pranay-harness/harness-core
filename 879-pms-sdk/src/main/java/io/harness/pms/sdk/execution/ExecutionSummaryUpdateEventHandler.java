package io.harness.pms.sdk.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.service.ExecutionSummaryUpdateRequest;
import io.harness.pms.contracts.service.PipelineModuleInfoProto;
import io.harness.pms.contracts.service.StageModuleInfoProto;
import io.harness.pms.sdk.PmsSdkModuleUtils;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.core.execution.PmsExecutionGrpcClient;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
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
      executionSummaryUpdateRequest.setNodeUuid(nodeExecutionProto.getAmbiance().getLevels(2).getSetupId());
    }
    if (Objects.equals(nodeExecutionProto.getNode().getGroup(), "STAGE")) {
      executionSummaryUpdateRequest.setNodeUuid(nodeExecutionProto.getNode().getUuid());
    }

    PipelineModuleInfo pipelineLevelModuleInfo =
        executionSummaryModuleInfoProvider.getPipelineLevelModuleInfo(nodeExecutionProto);
    String pipelineInfoJson = RecastOrchestrationUtils.toDocumentJson(pipelineLevelModuleInfo);
    if (EmptyPredicate.isNotEmpty(pipelineInfoJson)) {
      executionSummaryUpdateRequest.setPipelineModuleInfoJson(pipelineInfoJson);
    }
    if (pipelineLevelModuleInfo != null) {
      PipelineModuleInfoProto pipelineModuleInfoProto = pipelineLevelModuleInfo.toProto();
      if (pipelineModuleInfoProto != null) {
        executionSummaryUpdateRequest.setPipelineModuleInfo(pipelineModuleInfoProto);
      }
    }

    StageModuleInfo stageLevelModuleInfo =
        executionSummaryModuleInfoProvider.getStageLevelModuleInfo(nodeExecutionProto);
    String stageInfoJson = RecastOrchestrationUtils.toDocumentJson(stageLevelModuleInfo);
    if (EmptyPredicate.isNotEmpty(stageInfoJson)) {
      executionSummaryUpdateRequest.setNodeModuleInfoJson(stageInfoJson);
    }
    if (stageLevelModuleInfo != null) {
      StageModuleInfoProto stageModuleInfoProto = stageLevelModuleInfo.toProto();
      if (stageModuleInfoProto != null) {
        executionSummaryUpdateRequest.setStageModuleInfo(stageModuleInfoProto);
      }
    }

    pmsClient.updateExecutionSummary(executionSummaryUpdateRequest.build());
    log.info("Completed ExecutionSummaryUpdateEvent handler orchestration event of type [{}] for nodeExecutionId [{}]",
        orchestrationEvent.getEventType(), orchestrationEvent.getNodeExecutionProto().getStatus());
  }
}
