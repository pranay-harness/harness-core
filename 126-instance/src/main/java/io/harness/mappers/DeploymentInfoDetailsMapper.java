/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.instancesyncperpetualtaskinfo.DeploymentInfoDetailsDTO;
import io.harness.entities.instancesyncperpetualtaskinfo.DeploymentInfoDetails;
import io.harness.mappers.deploymentinfomapper.DeploymentInfoMapper;

import java.util.ArrayList;
import java.util.List;
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

  public List<DeploymentInfoDetails> toDeploymentInfoDetailsEntityList(
      List<DeploymentInfoDetailsDTO> deploymentInfoDetailsDTOList) {
    List<DeploymentInfoDetails> deploymentInfoDetailsList = new ArrayList<>();
    deploymentInfoDetailsDTOList.forEach(deploymentInfoDetailsDTO
        -> deploymentInfoDetailsList.add(DeploymentInfoDetailsMapper.toEntity(deploymentInfoDetailsDTO)));
    return deploymentInfoDetailsList;
  }

  public List<DeploymentInfoDetailsDTO> toDeploymentInfoDetailsDTOList(
      List<DeploymentInfoDetails> deploymentInfoDetailsList) {
    List<DeploymentInfoDetailsDTO> deploymentInfoDetailsDTOList = new ArrayList<>();
    deploymentInfoDetailsList.forEach(deploymentInfoDetails
        -> deploymentInfoDetailsDTOList.add(DeploymentInfoDetailsMapper.toDTO(deploymentInfoDetails)));
    return deploymentInfoDetailsDTOList;
  }
}
