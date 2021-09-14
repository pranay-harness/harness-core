/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.steps.approval.step.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.approval.step.beans.ApprovalInstanceDetailsDTO;

import io.swagger.annotations.ApiModel;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("HarnessApprovalInstanceDetails")
public class HarnessApprovalInstanceDetailsDTO implements ApprovalInstanceDetailsDTO {
  @NotNull String approvalMessage;
  boolean includePipelineExecutionHistory;
  @NotNull ApproversDTO approvers;
  List<HarnessApprovalActivity> approvalActivities;
  List<ApproverInputInfoDTO> approverInputs;
}
