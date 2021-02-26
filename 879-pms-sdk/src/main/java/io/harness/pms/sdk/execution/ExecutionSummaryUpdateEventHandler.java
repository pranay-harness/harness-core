package io.harness.pms.sdk.execution;

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

@Singleton
public class ExecutionSummaryUpdateEventHandler implements AsyncOrchestrationEventHandler {
  @Inject(optional = true) PmsExecutionGrpcClient pmsClient;
  @Inject(optional = true) ExecutionSummaryModuleInfoProvider executionSummaryModuleInfoProvider;
  @Inject @Named(PmsSdkModuleUtils.SDK_SERVICE_NAME) String serviceName;

  public ExecutionSummaryUpdateEventHandler() {}

  @Override
  public void handleEvent(OrchestrationEvent orchestrationEvent) {
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
  }
}
