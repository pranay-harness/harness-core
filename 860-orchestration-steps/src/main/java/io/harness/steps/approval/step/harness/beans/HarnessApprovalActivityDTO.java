/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.steps.approval.step.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "HarnessApprovalActivityKeys")
public class HarnessApprovalActivityDTO {
  @NotNull EmbeddedUserDTO user;
  @NotNull HarnessApprovalAction action;
  List<ApproverInput> approverInputs;
  String comments;
  Date approvedAt;

  public static HarnessApprovalActivityDTO fromHarnessApprovalActivity(HarnessApprovalActivity activity) {
    if (activity == null) {
      return null;
    }
    return HarnessApprovalActivityDTO.builder()
        .user(EmbeddedUserDTO.fromEmbeddedUser(activity.getUser()))
        .action(activity.getAction())
        .approverInputs(activity.getApproverInputs())
        .comments(activity.getComments())
        .approvedAt(activity.getApprovedAt() <= 0 ? null : new Date(activity.getApprovedAt()))
        .build();
  }
}
