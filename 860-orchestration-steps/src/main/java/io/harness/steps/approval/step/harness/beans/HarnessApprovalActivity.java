package io.harness.steps.approval.step.harness.beans;

import io.harness.beans.EmbeddedUser;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

/**
 * Captures data related to approving activity of a single user.
 */
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
