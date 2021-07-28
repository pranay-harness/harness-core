package io.harness.delegate.beans.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncPerpetualTaskResponse extends DelegateTaskNotifyResponseData {
  List<ServerInstanceInfo> getServerInstanceDetails();
}
