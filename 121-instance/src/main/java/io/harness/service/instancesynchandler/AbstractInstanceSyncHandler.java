package io.harness.service.instancesynchandler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.InstanceSyncPerpetualTaskResponse;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.entities.instanceinfo.InstanceInfo;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(HarnessTeam.DX)
public abstract class AbstractInstanceSyncHandler implements IInstanceSyncHandler {
  public abstract String getPerpetualTaskType();

  public abstract String getPerpetualTaskDescription(InfrastructureMappingDTO infrastructureMappingDTO);

  // Given list of deployment infos, check if the new deployment info is matching to any of it or not
  public abstract boolean isDeploymentInfoMatching(
      DeploymentInfoDTO newDeploymentInfoDTO, List<DeploymentInfoDTO> existingDeploymentInfoList);

  protected abstract InstanceInfo getInstanceInfoForServerInstance(ServerInstanceInfo serverInstanceInfo);

  public final List<InstanceInfo> getInstancesDetailsFromServerResponse(
      InstanceSyncPerpetualTaskResponse instanceSyncPerpetualTaskResponse) {
    List<InstanceInfo> instanceInfoList = new ArrayList<>();
    instanceSyncPerpetualTaskResponse.getServerInstanceDetails().forEach(
        serverInstanceInfo -> instanceInfoList.add(getInstanceInfoForServerInstance(serverInstanceInfo)));
    return instanceInfoList;
  }
}
