package io.harness.ci.plan.creator;

import io.harness.ci.plan.creator.execution.CIPipelineModuleInfo;
import io.harness.ci.plan.creator.execution.CIStageModuleInfo;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CIModuleInfoProvider implements ExecutionSummaryModuleInfoProvider {
  @Inject OutcomeService outcomeService;

  @Override
  public PipelineModuleInfo getPipelineLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    return CIPipelineModuleInfo.builder().build();
  }

  @Override
  public StageModuleInfo getStageLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    return CIStageModuleInfo.builder().build();
  }
}
