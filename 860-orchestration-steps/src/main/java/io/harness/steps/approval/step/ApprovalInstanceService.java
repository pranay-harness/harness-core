package io.harness.steps.approval.step;

import io.harness.beans.EmbeddedUser;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivityRequestDTO;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface ApprovalInstanceService {
  ApprovalInstance get(@NotNull String approvalInstanceId);
  void delete(@NotNull String approvalInstanceId);

  HarnessApprovalInstance expire(@NotNull String approvalInstanceId);
  void markExpiredInstances();

  HarnessApprovalInstance addHarnessApprovalActivity(@NotNull String approvalInstanceId, @NotNull EmbeddedUser user,
      @NotNull @Valid HarnessApprovalActivityRequestDTO request);
}
