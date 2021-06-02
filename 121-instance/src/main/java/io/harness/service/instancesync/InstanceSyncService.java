package io.harness.service.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.models.DeploymentEvent;

@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncService {
  void processDeploymentEvent(DeploymentEvent deploymentEvent);

  String manualSync(String accountId, String orgId, String projectId, String infrastructureMappingId);

  void processInstanceSyncResponseFromPerpetualTask(String perpetualTaskId, DelegateResponseData response);
}
