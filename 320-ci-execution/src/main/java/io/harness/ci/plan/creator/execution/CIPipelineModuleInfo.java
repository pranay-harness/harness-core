package io.harness.ci.plan.creator.execution;

import io.harness.ci.pipeline.executions.beans.CIWebhookInfoDTO;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CIPipelineModuleInfo implements PipelineModuleInfo {
  private CIWebhookInfoDTO ciWebhookInfoDTO;
}
