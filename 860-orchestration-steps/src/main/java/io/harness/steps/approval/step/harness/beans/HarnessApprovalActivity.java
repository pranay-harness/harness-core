/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.approval.step.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;

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
public class HarnessApprovalActivity {
  @NotNull EmbeddedUser user;
  @NotNull HarnessApprovalAction action;
  List<ApproverInput> approverInputs;
  String comments;
  long approvedAt;

  public static HarnessApprovalActivity fromHarnessApprovalActivityRequestDTO(
      EmbeddedUser user, HarnessApprovalActivityRequestDTO request) {
    if (user == null || request == null) {
      return null;
    }

    return HarnessApprovalActivity.builder()
        .user(user)
        .action(request.getAction())
        .approverInputs(request.getApproverInputs())
        .comments(request.getComments())
        .approvedAt(System.currentTimeMillis())
        .build();
  }
}
