package io.harness.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.instancesyncperpetualtaskinfo.DeploymentInfoDetailsDTO;
import io.harness.dtos.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoDTO;
import io.harness.entities.instancesyncperpetualtaskinfo.DeploymentInfoDetails;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class InstanceSyncPerpetualTaskInfoMapper {
  public InstanceSyncPerpetualTaskInfoDTO toDTO(InstanceSyncPerpetualTaskInfo instanceSyncPerpetualTaskInfo) {
    return InstanceSyncPerpetualTaskInfoDTO.builder()
        .id(instanceSyncPerpetualTaskInfo.getId())
        .accountIdentifier(instanceSyncPerpetualTaskInfo.getAccountIdentifier())
        .infrastructureMappingId(instanceSyncPerpetualTaskInfo.getInfrastructureMappingId())
        .deploymentInfoDetailsDTOList(
            toDeploymentInfoDetailsDTOList(instanceSyncPerpetualTaskInfo.getDeploymentInfoDetailsList()))
        .perpetualTaskId(instanceSyncPerpetualTaskInfo.getPerpetualTaskId())
        .createdAt(instanceSyncPerpetualTaskInfo.getCreatedAt())
        .lastUpdatedAt(instanceSyncPerpetualTaskInfo.getLastUpdatedAt())
        .build();
  }

  public InstanceSyncPerpetualTaskInfo toEntity(InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO) {
    return InstanceSyncPerpetualTaskInfo.builder()
        .id(instanceSyncPerpetualTaskInfoDTO.getId())
        .accountIdentifier(instanceSyncPerpetualTaskInfoDTO.getAccountIdentifier())
        .infrastructureMappingId(instanceSyncPerpetualTaskInfoDTO.getInfrastructureMappingId())
        .deploymentInfoDetailsList(
            toDeploymentInfoDetailsList(instanceSyncPerpetualTaskInfoDTO.getDeploymentInfoDetailsDTOList()))
        .perpetualTaskId(instanceSyncPerpetualTaskInfoDTO.getPerpetualTaskId())
        .build();
  }

  // ---------------------------------- PRIVATE METHODS ------------------------------------

  private List<DeploymentInfoDetails> toDeploymentInfoDetailsList(
      List<DeploymentInfoDetailsDTO> deploymentInfoDetailsDTOList) {
    List<DeploymentInfoDetails> deploymentInfoDetailsList = new ArrayList<>();
    deploymentInfoDetailsDTOList.forEach(deploymentInfoDetailsDTO
        -> deploymentInfoDetailsList.add(DeploymentInfoDetailsMapper.toEntity(deploymentInfoDetailsDTO)));
    return deploymentInfoDetailsList;
  }

  private List<DeploymentInfoDetailsDTO> toDeploymentInfoDetailsDTOList(
      List<DeploymentInfoDetails> deploymentInfoDetailsList) {
    List<DeploymentInfoDetailsDTO> deploymentInfoDetailsDTOList = new ArrayList<>();
    deploymentInfoDetailsList.forEach(deploymentInfoDetails
        -> deploymentInfoDetailsDTOList.add(DeploymentInfoDetailsMapper.toDTO(deploymentInfoDetails)));
    return deploymentInfoDetailsDTOList;
  }
}
