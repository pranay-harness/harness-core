package io.harness.pms.approval.beans;

import io.harness.beans.EmbeddedUser;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * Captures data related to approving activity of a single user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "HarnessApprovalActivityKeys")
public class HarnessApprovalActivity {
  EmbeddedUser user;
  ApprovalStatus status;
  List<ApproverInput> approverInputs;
  private String comments;
  long approvedAt;
}
