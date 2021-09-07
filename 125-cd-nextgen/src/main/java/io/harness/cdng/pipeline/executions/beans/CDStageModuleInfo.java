package io.harness.cdng.pipeline.executions.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
public class CDStageModuleInfo implements StageModuleInfo {
  ServiceExecutionSummary serviceInfo;
  InfraExecutionSummary infraExecutionSummary;
  String nodeExecutionId;
}
