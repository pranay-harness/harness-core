package io.harness.pms.sdk.execution;

import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.service.ExecutionSummaryUpdateRequest;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.core.execution.PmsExecutionGrpcClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.StatusRuntimeException;
import java.util.Objects;

@Singleton
public class ExecutionSummaryUpdateEventHandler implements AsyncOrchestrationEventHandler {
  @Inject(optional = true) PmsExecutionGrpcClient pmsClient;
  @Inject(optional = true) ExecutionSummaryModuleInfoProvider executionSummaryModuleInfoProvider;

  public ExecutionSummaryUpdateEventHandler() {}

  @Override
  public void handleEvent(OrchestrationEvent orchestrationEvent) {
    NodeExecutionProto nodeExecutionProto = orchestrationEvent.getNodeExecutionProto();
    ExecutionSummaryUpdateRequest.Builder executionSummaryUpdateRequest =
        ExecutionSummaryUpdateRequest.newBuilder()
            .setModuleName("cd")
            .setPlanExecutionId(nodeExecutionProto.getAmbiance().getPlanExecutionId())
            .setPipelineModuleInfoJson(
                executionSummaryModuleInfoProvider.getPipelineLevelModuleInfo(nodeExecutionProto).toJson())
            .setNodeModuleInfoJson(
                executionSummaryModuleInfoProvider.getStageLevelModuleInfo(nodeExecutionProto).toJson())
            .setNodeExecutionId(nodeExecutionProto.getUuid());
    if (Objects.equals(nodeExecutionProto.getNode().getGroup(), "stage")) {
      executionSummaryUpdateRequest.setNodeUuid(nodeExecutionProto.getNode().getIdentifier());
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
