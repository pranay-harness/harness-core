package io.harness.pms.plan.execution;

import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class PmsPipelineModuleInfo implements PipelineModuleInfo {
  @Singular Set<String> approvalStageNames;
  boolean hasApprovalStage;
}
