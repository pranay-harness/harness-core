/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mappers.deploymentinfomapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.K8sDeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.ReferenceK8sPodInfoDTO;
import io.harness.entities.deploymentinfo.DeploymentInfo;
import io.harness.entities.deploymentinfo.K8sDeploymentInfo;
import io.harness.entities.deploymentinfo.ReferenceK8sPodInfo;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class DeploymentInfoMapper {
  public DeploymentInfoDTO toDTO(DeploymentInfo deploymentInfo) {
    if (deploymentInfo instanceof ReferenceK8sPodInfo) {
      return ReferenceK8sPodInfoMapper.toDTO((ReferenceK8sPodInfo) deploymentInfo);
    } else if (deploymentInfo instanceof K8sDeploymentInfo) {
      return K8sDeploymentInfoMapper.toDTO((K8sDeploymentInfo) deploymentInfo);
    }
    throw new InvalidRequestException("No DeploymentInfoMapper toDTO found for deploymentInfo : {}" + deploymentInfo);
  }

  public DeploymentInfo toEntity(DeploymentInfoDTO deploymentInfoDTO) {
    if (deploymentInfoDTO instanceof ReferenceK8sPodInfoDTO) {
      return ReferenceK8sPodInfoMapper.toEntity((ReferenceK8sPodInfoDTO) deploymentInfoDTO);
    } else if (deploymentInfoDTO instanceof K8sDeploymentInfoDTO) {
      return K8sDeploymentInfoMapper.toEntity((K8sDeploymentInfoDTO) deploymentInfoDTO);
    }
    throw new InvalidRequestException(
        "No DeploymentInfoMapper toEntity found for deploymentInfo : {}" + deploymentInfoDTO);
  }
}
