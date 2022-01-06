/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.mappers.deploymentinfomapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.ReferenceK8sPodInfoDTO;
import io.harness.entities.deploymentinfo.ReferenceK8sPodInfo;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class ReferenceK8sPodInfoMapper {
  public ReferenceK8sPodInfoDTO toDTO(ReferenceK8sPodInfo referenceK8sPodInfo) {
    return ReferenceK8sPodInfoDTO.builder().podName(referenceK8sPodInfo.getPodName()).build();
  }

  public ReferenceK8sPodInfo toEntity(ReferenceK8sPodInfoDTO referenceK8sPodInfoDTO) {
    return ReferenceK8sPodInfo.builder().podName(referenceK8sPodInfoDTO.getPodName()).build();
  }
}
