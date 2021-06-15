package io.harness.pms.sample.cd;

import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

public class CDExecutionSummaryModuleInfoProvider implements ExecutionSummaryModuleInfoProvider {
  @Override
  public PipelineModuleInfo getPipelineLevelModuleInfo(OrchestrationEvent event) {
    return new SamplePipelineModule();
  }

  @Override
  public StageModuleInfo getStageLevelModuleInfo(OrchestrationEvent event) {
    return new SampleStageInfo();
  }

  public class SamplePipelineModule implements PipelineModuleInfo {}

  public class SampleStageInfo implements StageModuleInfo {}
}
