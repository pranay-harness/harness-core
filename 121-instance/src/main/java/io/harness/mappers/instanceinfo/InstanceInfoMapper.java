/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mappers.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.instanceinfo.InstanceInfoDTO;
import io.harness.dtos.instanceinfo.K8sInstanceInfoDTO;
import io.harness.dtos.instanceinfo.ReferenceInstanceInfoDTO;
import io.harness.entities.instanceinfo.InstanceInfo;
import io.harness.entities.instanceinfo.K8sInstanceInfo;
import io.harness.entities.instanceinfo.ReferenceInstanceInfo;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class InstanceInfoMapper {
  public InstanceInfoDTO toDTO(InstanceInfo instanceInfo) {
    if (instanceInfo instanceof ReferenceInstanceInfo) {
      return ReferenceInstanceInfoMapper.toDTO((ReferenceInstanceInfo) instanceInfo);
    } else if (instanceInfo instanceof K8sInstanceInfo) {
      return K8sInstanceInfoMapper.toDTO((K8sInstanceInfo) instanceInfo);
    }
    throw new InvalidRequestException("No InstanceInfoMapper toDTO found for instanceInfo : {}" + instanceInfo);
  }

  public InstanceInfo toEntity(InstanceInfoDTO instanceInfoDTO) {
    if (instanceInfoDTO instanceof ReferenceInstanceInfoDTO) {
      return ReferenceInstanceInfoMapper.toEntity((ReferenceInstanceInfoDTO) instanceInfoDTO);
    } else if (instanceInfoDTO instanceof K8sInstanceInfoDTO) {
      return K8sInstanceInfoMapper.toEntity((K8sInstanceInfoDTO) instanceInfoDTO);
    }
    throw new InvalidRequestException(
        "No InstanceInfoMapper toEntity found for instanceInfoDTO : {}" + instanceInfoDTO);
  }
}
