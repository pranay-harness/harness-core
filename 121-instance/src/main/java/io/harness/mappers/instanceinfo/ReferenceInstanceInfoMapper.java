/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mappers.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.dtos.instanceinfo.ReferenceInstanceInfoDTO;
import io.harness.entities.instanceinfo.ReferenceInstanceInfo;

import lombok.experimental.UtilityClass;

@io.harness.annotations.dev.OwnedBy(HarnessTeam.DX)
@UtilityClass
public class ReferenceInstanceInfoMapper {
  public ReferenceInstanceInfoDTO toDTO(ReferenceInstanceInfo referenceInstanceInfo) {
    return ReferenceInstanceInfoDTO.builder().podName(referenceInstanceInfo.getPodName()).build();
  }

  public ReferenceInstanceInfo toEntity(ReferenceInstanceInfoDTO referenceInstanceInfoDTO) {
    return ReferenceInstanceInfo.builder().podName(referenceInstanceInfoDTO.getPodName()).build();
  }
}
