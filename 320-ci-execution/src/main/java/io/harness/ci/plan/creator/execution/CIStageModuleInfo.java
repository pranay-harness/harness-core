package io.harness.ci.plan.creator.execution;

import io.harness.pms.sdk.execution.beans.StageModuleInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CIStageModuleInfo implements StageModuleInfo {
  String nodeExecutionId;
}
