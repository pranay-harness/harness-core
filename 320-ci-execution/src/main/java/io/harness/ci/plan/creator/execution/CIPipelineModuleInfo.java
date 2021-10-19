package io.harness.ci.plan.creator.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.pipeline.executions.beans.CIWebhookInfoDTO;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CI)
public class CIPipelineModuleInfo implements PipelineModuleInfo {
  private CIWebhookInfoDTO ciExecutionInfoDTO;
  private String branch;
  private String repoName;
  private String triggerRepoName;
  private String tag;
  private String prNumber;
  private String buildType;
}
