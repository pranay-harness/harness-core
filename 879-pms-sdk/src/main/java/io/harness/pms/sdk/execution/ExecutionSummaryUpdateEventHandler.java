package io.harness.pms.sdk.execution;

import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.service.ExecutionSummaryUpdateRequest;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

import com.google.inject.Injector;
import io.grpc.StatusRuntimeException;

public abstract class ExecutionSummaryUpdateEventHandler implements AsyncOrchestrationEventHandler {
  Injector injector;

  public ExecutionSummaryUpdateEventHandler(Injector injector) {
    this.injector = injector;
  }
  public abstract PipelineModuleInfo getPipelineLevelModuleInfo(NodeExecutionProto nodeExecutionProto);

  public abstract StageModuleInfo getStageLevelModuleInfo(NodeExecutionProto nodeExecutionProto);

  @Override
  public void handleEvent(OrchestrationEvent orchestrationEvent) {
    NodeExecutionProto nodeExecutionProto = NodeExecutionProto.newBuilder().build();
    ExecutionSummaryUpdateRequest.Builder executionSummaryUpdateRequest =
        ExecutionSummaryUpdateRequest.newBuilder()
            .setModuleName("cd")
            .setPlanExecutionId(nodeExecutionProto.getAmbiance().getPlanExecutionId())
            .setPipelineModuleInfoJson(getPipelineLevelModuleInfo(nodeExecutionProto).toJson())
            .setStageModuleInfoJson(getStageLevelModuleInfo(nodeExecutionProto).toJson());
    if (nodeExecutionProto.getNode().getGroup().equalsIgnoreCase("stage")) {
      executionSummaryUpdateRequest.setStageUuid(nodeExecutionProto.getNode().getUuid());
    }
    if (nodeExecutionProto.getNode().getGroup().equalsIgnoreCase("stage")
        || nodeExecutionProto.getNode().getGroup().equalsIgnoreCase("pipeline")) {
      executionSummaryUpdateRequest.setStatus(nodeExecutionProto.getStatus());
    }
    try {
      PmsExecutionServiceGrpc.PmsExecutionServiceBlockingStub pmsClient =
          injector.getInstance(PmsExecutionServiceGrpc.PmsExecutionServiceBlockingStub.class);
      pmsClient.updateExecutionSummary(executionSummaryUpdateRequest.build());
    } catch (StatusRuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw ex;
    }
  }
}
