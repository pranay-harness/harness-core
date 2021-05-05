package io.harness.service.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.entity.DeploymentEvent;

@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncService {
  void processDeploymentEvent(DeploymentEvent deploymentEvent);

  String manualSync(String appId, String infraMappingId);

  void processInstanceSyncResponseFromPerpetualTask(String perpetualTaskId, DelegateResponseData response);
}
