/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mappers.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.instanceinfo.K8sInstanceInfoDTO;
import io.harness.entities.instanceinfo.K8sInstanceInfo;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class K8sInstanceInfoMapper {
  public K8sInstanceInfoDTO toDTO(K8sInstanceInfo k8sInstanceInfo) {
    return K8sInstanceInfoDTO.builder()
        .blueGreenColor(k8sInstanceInfo.getBlueGreenColor())
        .containerList(k8sInstanceInfo.getContainerList())
        .namespace(k8sInstanceInfo.getNamespace())
        .podIP(k8sInstanceInfo.getPodIP())
        .podName(k8sInstanceInfo.getPodName())
        .releaseName(k8sInstanceInfo.getReleaseName())
        .build();
  }

  public K8sInstanceInfo toEntity(K8sInstanceInfoDTO k8sInstanceInfoDTO) {
    return K8sInstanceInfo.builder()
        .blueGreenColor(k8sInstanceInfoDTO.getBlueGreenColor())
        .containerList(k8sInstanceInfoDTO.getContainerList())
        .namespace(k8sInstanceInfoDTO.getNamespace())
        .podIP(k8sInstanceInfoDTO.getPodIP())
        .podName(k8sInstanceInfoDTO.getPodName())
        .releaseName(k8sInstanceInfoDTO.getReleaseName())
        .build();
  }
}
