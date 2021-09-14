/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
