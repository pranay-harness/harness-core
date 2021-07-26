package io.harness.service.instancesynchandler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.dtos.instanceinfo.InstanceInfoDTO;
import io.harness.entities.InstanceType;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(HarnessTeam.DX)
public abstract class AbstractInstanceSyncHandler implements IInstanceSyncHandler {
  /**
   * Refer {@link io.harness.perpetualtask.PerpetualTaskType}
   * We need to do similar in NG. Every handler must return appropriate task type
   */
  public abstract String getPerpetualTaskType();

  /**
   * Return informative textual description based on the deployment type for the perpetual task
   */
  public abstract String getPerpetualTaskDescription(InfrastructureMappingDTO infrastructureMappingDTO);

  public abstract InstanceType getInstanceType();

  protected abstract InstanceInfoDTO getInstanceInfoForServerInstance(ServerInstanceInfo serverInstanceInfo);

  public final List<InstanceInfoDTO> getInstanceDetailsFromServerInstances(
      List<ServerInstanceInfo> serverInstanceInfoList) {
    List<InstanceInfoDTO> instanceInfoList = new ArrayList<>();
    serverInstanceInfoList.forEach(
        serverInstanceInfo -> instanceInfoList.add(getInstanceInfoForServerInstance(serverInstanceInfo)));
    return instanceInfoList;
  }
}
