package io.harness.pms.plan.execution;

import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

import lombok.Builder;
import lombok.Data;

public class PmsExecutionServiceInfoProvider implements ExecutionSummaryModuleInfoProvider {
  @Override
  public PipelineModuleInfo getPipelineLevelModuleInfo(OrchestrationEvent event) {
    return PmsNoopModuleInfo.builder().build();
  }

  @Override
  public StageModuleInfo getStageLevelModuleInfo(OrchestrationEvent event) {
    return PmsNoopModuleInfo.builder().build();
  }

  @Data
  @Builder
  public static class PmsNoopModuleInfo implements PipelineModuleInfo, StageModuleInfo {}
}
