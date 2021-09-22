package io.harness.service.instancesyncperpetualtaskstatus;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public interface InstanceSyncPerpetualTaskStatusService {
  boolean handlePerpetualTaskFailure(String perpetualTaskId, String errorMessage);

  void updatePerpetualTaskSuccess(String perpetualTaskId);
}
