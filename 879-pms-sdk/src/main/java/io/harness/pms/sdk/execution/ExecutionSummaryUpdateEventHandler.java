package io.harness.pms.sdk.execution;

import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

public abstract class ExecutionSummaryUpdateEventHandler implements AsyncOrchestrationEventHandler {
  public abstract PipelineModuleInfo getPipelineLevelModuleInfo(NodeExecutionProto nodeExecutionProto);

  public abstract StageModuleInfo getStageLevelModuleInfo(NodeExecutionProto nodeExecutionProto);

  @Override
  public void handleEvent(OrchestrationEvent orchestrationEvent) {
    NodeExecutionProto nodeExecutionProto = NodeExecutionProto.newBuilder().build();
    PipelineModuleInfo pipelineModuleInfo = getPipelineLevelModuleInfo(nodeExecutionProto);
    // todo: Add grpc call over here
  }
}
