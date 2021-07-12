package io.harness.service.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.models.DeploymentEvent;

@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncService {
  void processInstanceSyncForNewDeployment(DeploymentEvent deploymentEvent);
}
