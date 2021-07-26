package io.harness.service.instancesynchandler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.dtos.instanceinfo.InstanceInfoDTO;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public interface IInstanceSyncHandler {
  // Maps server instance dtos to the instance info dtos required for the instance entities
  List<InstanceInfoDTO> getInstanceDetailsFromServerInstances(List<ServerInstanceInfo> serverInstanceInfoList);

  // Create unique string instance key param from instance info that uniquely identifies the particular instance
  String getInstanceKey(InstanceInfoDTO instanceInfoDTO);

  // Get instance sync handler key from instance info that could relate all instances corresponding to a deployment info
  String getInstanceSyncHandlerKey(InstanceInfoDTO instanceInfoDTO);

  String getInstanceSyncHandlerKey(DeploymentInfoDTO deploymentInfoDTO);
}
