package io.harness.ci.plan.creator.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.pipeline.executions.beans.CIWebhookInfoDTO;
import io.harness.pms.contracts.ci.CIPipelineModuleInfoProto;
import io.harness.pms.contracts.service.PipelineModuleInfoProto;
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
  private String tag;

  @Override
  public PipelineModuleInfoProto toProto() {
    CIPipelineModuleInfoProto.Builder builder = CIPipelineModuleInfoProto.newBuilder();
    if (ciExecutionInfoDTO != null) {
      builder.setCiExecutionInfo(ciExecutionInfoDTO.toProto());
    }
    if (branch != null) {
      builder.setBranch(branch);
    }
    if (repoName != null) {
      builder.setRepoName(repoName);
    }
    if (tag != null) {
      builder.setTag(tag);
    }
    return null;
  }
}
