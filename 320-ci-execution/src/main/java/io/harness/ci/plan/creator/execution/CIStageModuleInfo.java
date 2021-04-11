package io.harness.ci.plan.creator.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CI)
public class CIStageModuleInfo implements StageModuleInfo {
  String nodeExecutionId;
}
