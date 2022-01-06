package io.harness.mappers.deploymentinfomapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.NativeHelmDeploymentInfoDTO;
import io.harness.entities.deploymentinfo.NativeHelmDeploymentInfo;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class NativeHelmDeploymentInfoMapper {
  public NativeHelmDeploymentInfoDTO toDTO(NativeHelmDeploymentInfo nativeHelmDeploymentInfo) {
    return NativeHelmDeploymentInfoDTO.builder()
        .namespaces(nativeHelmDeploymentInfo.getNamespaces())
        .releaseName(nativeHelmDeploymentInfo.getReleaseName())
        .helmChartInfo(nativeHelmDeploymentInfo.getHelmChartInfo())
        .helmVersion(nativeHelmDeploymentInfo.getHelmVersion())
        .build();
  }

  public NativeHelmDeploymentInfo toEntity(NativeHelmDeploymentInfoDTO nativeHelmDeploymentInfoDTO) {
    return NativeHelmDeploymentInfo.builder()
        .namespaces(nativeHelmDeploymentInfoDTO.getNamespaces())
        .releaseName(nativeHelmDeploymentInfoDTO.getReleaseName())
        .helmChartInfo(nativeHelmDeploymentInfoDTO.getHelmChartInfo())
        .helmVersion(nativeHelmDeploymentInfoDTO.getHelmVersion())
        .build();
  }
}
