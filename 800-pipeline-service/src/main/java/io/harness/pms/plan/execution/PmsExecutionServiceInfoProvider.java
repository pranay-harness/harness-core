package io.harness.pms.plan.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsExecutionServiceInfoProvider implements ExecutionSummaryModuleInfoProvider {
  @Override
  public PipelineModuleInfo getPipelineLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    return PmsNoopPipelineModuleInfo.builder().build();
  }

  @Override
  public StageModuleInfo getStageLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    return PmsNoopStageModuleInfo.builder().build();
  }

  @Data
  @Builder
  public static class PmsNoopPipelineModuleInfo implements PipelineModuleInfo {}

  @Data
  @Builder
  public static class PmsNoopStageModuleInfo implements StageModuleInfo {}
}
