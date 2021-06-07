package io.harness.pms.sdk.core.execution;

import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

public interface ExecutionSummaryModuleInfoProvider {
  PipelineModuleInfo getPipelineLevelModuleInfo(OrchestrationEvent event);
  StageModuleInfo getStageLevelModuleInfo(OrchestrationEvent event);
}
