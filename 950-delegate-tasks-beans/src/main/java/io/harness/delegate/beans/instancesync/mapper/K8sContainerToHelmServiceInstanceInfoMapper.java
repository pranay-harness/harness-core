/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.instancesync.mapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.container.ContainerInfo;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.NativeHelmServerInstanceInfo;
import io.harness.delegate.task.helm.HelmChartInfo;
import io.harness.k8s.model.HelmVersion;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CDP)
public class K8sContainerToHelmServiceInstanceInfoMapper {
  public List<ServerInstanceInfo> toServerInstanceInfoList(
      List<ContainerInfo> containerInfoList, HelmChartInfo chartInfo, HelmVersion helmVersion) {
    return containerInfoList.stream()
        .map(containerInfo -> toServerInstanceInfo(containerInfo, chartInfo, helmVersion))
        .collect(Collectors.toList());
  }

  public ServerInstanceInfo toServerInstanceInfo(
      ContainerInfo containerInfo, HelmChartInfo chartInfo, HelmVersion helmVersion) {
    return NativeHelmServerInstanceInfo.builder()
        .podName(containerInfo.getPodName())
        .namespace(containerInfo.getNamespace())
        .releaseName(containerInfo.getReleaseName())
        .ip(containerInfo.getIp())
        .helmChartInfo(chartInfo)
        .helmVersion(helmVersion)
        .build();
  }
}
