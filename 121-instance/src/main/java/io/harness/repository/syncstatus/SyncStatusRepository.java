package io.harness.repository.syncstatus;

import io.harness.dto.SyncStatus;

public interface SyncStatusRepository {
  SyncStatus getSyncStatus(
      String orgId, String projectId, String serviceId, String envId, String infrastructureMappingId);

  void deleteById(String syncStatusId);
}
