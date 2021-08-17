package io.harness.service.instancesyncperpetualtask;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;

import java.util.List;

@OwnedBy(DX)
public interface InstanceSyncPerpetualTaskService {
  String createPerpetualTask(InfrastructureMappingDTO infrastructureMappingDTO,
      AbstractInstanceSyncHandler abstractInstanceSyncHandler, List<DeploymentInfoDTO> deploymentInfoDTOList,
      InfrastructureOutcome infrastructureOutcome);

  void resetPerpetualTask(String accountIdentifier, String perpetualTaskId,
      InfrastructureMappingDTO infrastructureMappingDTO, AbstractInstanceSyncHandler abstractInstanceSyncHandler,
      List<DeploymentInfoDTO> deploymentInfoDTOList, InfrastructureOutcome infrastructureOutcome);

  void deletePerpetualTask(String accountIdentifier, String perpetualTaskId);
}
