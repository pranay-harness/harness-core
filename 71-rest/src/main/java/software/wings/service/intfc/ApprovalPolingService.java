package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.approval.ApprovalPollingJobEntity;

@OwnedBy(CDC)
public interface ApprovalPolingService {
  String save(ApprovalPollingJobEntity approvalPollingJobEntity);
  void delete(String entityId);
}
