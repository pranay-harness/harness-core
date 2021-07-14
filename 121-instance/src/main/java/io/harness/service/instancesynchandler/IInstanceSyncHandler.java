package io.harness.service.instancesynchandler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.InstanceSyncPerpetualTaskResponse;
import io.harness.entities.instanceinfo.InstanceInfo;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public interface IInstanceSyncHandler {
  List<InstanceInfo> getInstancesDetailsFromServerResponse(
      InstanceSyncPerpetualTaskResponse instanceSyncPerpetualTaskResponse);
}
