package io.harness.service.instancesyncperpetualtask;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;

@OwnedBy(DX)
public interface InstanceSyncPerpetualTaskService {
  String createPerpetualTask(
      InfrastructureMappingDTO infrastructureMappingDTO, AbstractInstanceSyncHandler abstractInstanceSyncHandler);

  void resetPerpetualTask(String accountIdentifier, String perpetualTaskId);

  void deletePerpetualTask(String accountIdentifier, String perpetualTaskId);
}
