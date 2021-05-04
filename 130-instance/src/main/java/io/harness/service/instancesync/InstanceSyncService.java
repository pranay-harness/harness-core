package io.harness.service.instancesync;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.entity.DeploymentEvent;

public interface InstanceSyncService {
  void processDeploymentEvent(DeploymentEvent deploymentEvent);

  String manualSync(String appId, String infraMappingId);

  void processInstanceSyncResponseFromPerpetualTask(String perpetualTaskId, DelegateResponseData response);
}
