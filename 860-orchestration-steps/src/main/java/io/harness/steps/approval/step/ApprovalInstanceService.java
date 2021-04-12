package io.harness.steps.approval.step;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivityRequestDTO;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface ApprovalInstanceService {
  ApprovalInstance save(@NotNull ApprovalInstance instance);
  ApprovalInstance get(@NotNull String approvalInstanceId);
  void delete(@NotNull String approvalInstanceId);

  void expireByNodeExecutionId(@NotNull String approvalInstanceId);
  void markExpiredInstances();

  void finalizeStatus(@NotNull String approvalInstanceId, ApprovalStatus status);
  HarnessApprovalInstance addHarnessApprovalActivity(@NotNull String approvalInstanceId, @NotNull EmbeddedUser user,
      @NotNull @Valid HarnessApprovalActivityRequestDTO request);
}
