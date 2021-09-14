/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoDTO;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class InstanceSyncPerpetualTaskInfoMapper {
  public InstanceSyncPerpetualTaskInfoDTO toDTO(InstanceSyncPerpetualTaskInfo instanceSyncPerpetualTaskInfo) {
    return InstanceSyncPerpetualTaskInfoDTO.builder()
        .id(instanceSyncPerpetualTaskInfo.getId())
        .accountIdentifier(instanceSyncPerpetualTaskInfo.getAccountIdentifier())
        .infrastructureMappingId(instanceSyncPerpetualTaskInfo.getInfrastructureMappingId())
        .deploymentInfoDetailsDTOList(DeploymentInfoDetailsMapper.toDeploymentInfoDetailsDTOList(
            instanceSyncPerpetualTaskInfo.getDeploymentInfoDetailsList()))
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
        .deploymentInfoDetailsList(DeploymentInfoDetailsMapper.toDeploymentInfoDetailsEntityList(
            instanceSyncPerpetualTaskInfoDTO.getDeploymentInfoDetailsDTOList()))
        .perpetualTaskId(instanceSyncPerpetualTaskInfoDTO.getPerpetualTaskId())
        .build();
  }
}
