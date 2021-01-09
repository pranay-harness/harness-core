package io.harness.pms.plan.execution;

import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

import lombok.Builder;
import lombok.Data;

public class PmsExecutionServiceInfoProvider implements ExecutionSummaryModuleInfoProvider {
  @Override
  public PipelineModuleInfo getPipelineLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    return PmsNoopModuleInfo.builder().build();
  }

  @Override
  public StageModuleInfo getStageLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    return PmsNoopModuleInfo.builder().build();
  }

  @Data
  @Builder
  public static class PmsNoopModuleInfo implements PipelineModuleInfo, StageModuleInfo {}
}
