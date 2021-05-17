package io.harness.repositories.instancesyncperpetualtask;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncPerpetualTaskRepositoryCustom {
  void save(String accountId, String infrastructureMappingId, List<String> perpetualTaskIds);
}
