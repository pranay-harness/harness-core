package software.wings.service.intfc;

import software.wings.beans.approval.ApprovalPollingJobEntity;

public interface ApprovalPolingService {
  String save(ApprovalPollingJobEntity approvalPollingJobEntity);
  void delete(String entityId);
}
