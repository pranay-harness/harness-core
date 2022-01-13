/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
