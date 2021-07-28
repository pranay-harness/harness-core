package io.harness.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.instancesyncperpetualtaskinfo.DeploymentInfoDetailsDTO;
import io.harness.entities.instancesyncperpetualtaskinfo.DeploymentInfoDetails;
import io.harness.mappers.deploymentinfomapper.DeploymentInfoMapper;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class DeploymentInfoDetailsMapper {
  public DeploymentInfoDetailsDTO toDTO(DeploymentInfoDetails deploymentInfoDetails) {
    return DeploymentInfoDetailsDTO.builder()
        .deploymentInfoDTO(DeploymentInfoMapper.toDTO(deploymentInfoDetails.getDeploymentInfo()))
        .lastUsedAt(deploymentInfoDetails.getLastUsedAt())
        .build();
  }

  public DeploymentInfoDetails toEntity(DeploymentInfoDetailsDTO deploymentInfoDetailsDTO) {
    return DeploymentInfoDetails.builder()
        .deploymentInfo(DeploymentInfoMapper.toEntity(deploymentInfoDetailsDTO.getDeploymentInfoDTO()))
        .lastUsedAt(deploymentInfoDetailsDTO.getLastUsedAt())
        .build();
  }
}
