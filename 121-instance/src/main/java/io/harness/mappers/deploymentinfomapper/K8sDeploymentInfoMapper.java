/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mappers.deploymentinfomapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.K8sDeploymentInfoDTO;
import io.harness.entities.deploymentinfo.K8sDeploymentInfo;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class K8sDeploymentInfoMapper {
  public K8sDeploymentInfoDTO toDTO(K8sDeploymentInfo k8sDeploymentInfo) {
    return K8sDeploymentInfoDTO.builder()
        .blueGreenStageColor(k8sDeploymentInfo.getBlueGreenStageColor())
        .namespaces(k8sDeploymentInfo.getNamespaces())
        .releaseName(k8sDeploymentInfo.getReleaseName())
        .build();
  }

  public K8sDeploymentInfo toEntity(K8sDeploymentInfoDTO k8sDeploymentInfoDTO) {
    return K8sDeploymentInfo.builder()
        .blueGreenStageColor(k8sDeploymentInfoDTO.getBlueGreenStageColor())
        .namespaces(k8sDeploymentInfoDTO.getNamespaces())
        .releaseName(k8sDeploymentInfoDTO.getReleaseName())
        .build();
  }
}
