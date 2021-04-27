package io.harness.instancesync.repository.instancesyncperpetualtask;

import io.harness.instancesync.dto.InstanceSyncPerpetualTaskInfo;

import java.util.List;
import java.util.Optional;

public interface InstanceSyncPerpetualTaskRepository {
  Optional<InstanceSyncPerpetualTaskInfo> getByAccountIdAndInfrastructureMappingId(
      String accountId, String inframappingId);

  void save(String accountId, String infrastructureMappingId, List<String> perpetualTaskIds);

  void save(InstanceSyncPerpetualTaskInfo info);

  void delete(String infrastructureMappingId);
}
