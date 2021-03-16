package io.harness.steps.approval.step;

import io.harness.beans.EmbeddedUser;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivityRequestDTO;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance;

import javax.validation.constraints.NotNull;

public interface ApprovalInstanceService {
  ApprovalInstance get(@NotNull String approvalInstanceId);
  void delete(@NotNull String approvalInstanceId);
  HarnessApprovalInstance addHarnessApprovalActivity(@NotNull String approvalInstanceId, @NotNull ApprovalStatus status,
      @NotNull EmbeddedUser user, @NotNull HarnessApprovalActivityRequestDTO request);
}
