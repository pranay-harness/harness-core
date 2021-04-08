package io.harness.steps.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance;

@OwnedBy(CDC)
public interface ApprovalNotificationHandler {
  void sendNotification(HarnessApprovalInstance approvalInstance, Ambiance ambiance);
}
