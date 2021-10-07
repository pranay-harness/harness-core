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
