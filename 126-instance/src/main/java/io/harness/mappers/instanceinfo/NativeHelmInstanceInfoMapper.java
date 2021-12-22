package io.harness.mappers.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.instanceinfo.NativeHelmInstanceInfoDTO;
import io.harness.entities.instanceinfo.NativeHelmInstanceInfo;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class NativeHelmInstanceInfoMapper {
    public NativeHelmInstanceInfoDTO toDTO(NativeHelmInstanceInfo nativeHelmInstanceInfo) {
        return NativeHelmInstanceInfoDTO.builder()
                .namespace(nativeHelmInstanceInfo.getNamespace())
                .ip(nativeHelmInstanceInfo.getIp())
                .podName(nativeHelmInstanceInfo.getPodName())
                .releaseName(nativeHelmInstanceInfo.getReleaseName())
                .helmChartInfo(nativeHelmInstanceInfo.getHelmChartInfo())
                .helmVersion(nativeHelmInstanceInfo.getHelmVersion())
                .build();
    }

    public NativeHelmInstanceInfo toEntity(NativeHelmInstanceInfoDTO nativeHelmInstanceInfoDTO) {
        return NativeHelmInstanceInfo.builder()
                .namespace(nativeHelmInstanceInfoDTO.getNamespace())
                .ip(nativeHelmInstanceInfoDTO.getIp())
                .podName(nativeHelmInstanceInfoDTO.getPodName())
                .releaseName(nativeHelmInstanceInfoDTO.getReleaseName())
                .helmChartInfo(nativeHelmInstanceInfoDTO.getHelmChartInfo())
                .helmVersion(nativeHelmInstanceInfoDTO.getHelmVersion())
                .build();
    }
}
