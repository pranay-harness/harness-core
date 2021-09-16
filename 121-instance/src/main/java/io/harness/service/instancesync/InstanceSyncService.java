package io.harness.service.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.InstanceSyncPerpetualTaskResponse;
import io.harness.models.DeploymentEvent;

@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncService {
  void processInstanceSyncForNewDeployment(DeploymentEvent deploymentEvent);

  void processInstanceSyncByPerpetualTask(String accountIdentifier, String perpetualTaskId,
      InstanceSyncPerpetualTaskResponse instanceSyncPerpetualTaskResponse);
}
