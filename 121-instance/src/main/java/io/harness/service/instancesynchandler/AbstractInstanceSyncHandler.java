package io.harness.service.instancesynchandler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.infrastructuremapping.InfrastructureMappingDTO;

@OwnedBy(HarnessTeam.DX)
public abstract class AbstractInstanceSyncHandler implements IInstanceSyncHandler {
  public abstract String getPerpetualTaskType();
  public abstract String getPerpetualTaskDescription(InfrastructureMappingDTO infrastructureMappingDTO);
}
