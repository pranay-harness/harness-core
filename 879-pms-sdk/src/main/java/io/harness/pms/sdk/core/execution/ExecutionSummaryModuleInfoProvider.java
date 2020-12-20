package io.harness.pms.sdk.core.execution;

import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

public interface ExecutionSummaryModuleInfoProvider {
  PipelineModuleInfo getPipelineLevelModuleInfo(NodeExecutionProto nodeExecutionProto);
  StageModuleInfo getStageLevelModuleInfo(NodeExecutionProto nodeExecutionProto);
}
