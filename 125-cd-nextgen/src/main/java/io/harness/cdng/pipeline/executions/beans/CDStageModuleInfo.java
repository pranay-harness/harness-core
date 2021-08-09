package io.harness.cdng.pipeline.executions.beans;

import io.harness.ngpipeline.pipeline.executions.beans.ServiceExecutionSummary;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CDStageModuleInfo implements StageModuleInfo {
  ServiceExecutionSummary serviceInfo;
  InfraExecutionSummary infraExecutionSummary;
  String nodeExecutionId;
}
