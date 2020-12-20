package io.harness.pms.sample.cd;

import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

public class CDExecutionSummaryModuleInfoProvider implements ExecutionSummaryModuleInfoProvider {
  @Override
  public PipelineModuleInfo getPipelineLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    return new SamplePipelineModule();
  }

  @Override
  public StageModuleInfo getStageLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    return new SampleStageInfo();
  }

  public class SamplePipelineModule implements PipelineModuleInfo {}

  public class SampleStageInfo implements StageModuleInfo {}
}
