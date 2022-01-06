/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.instancesync.mapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.K8sServerInstanceInfo;
import io.harness.k8s.model.K8sPod;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CDP)
public class K8sPodToServiceInstanceInfoMapper {
  public List<ServerInstanceInfo> toServerInstanceInfoList(List<K8sPod> k8sPodList) {
    return k8sPodList.stream()
        .map(K8sPodToServiceInstanceInfoMapper::toServerInstanceInfo)
        .collect(Collectors.toList());
  }

  public ServerInstanceInfo toServerInstanceInfo(K8sPod k8sPod) {
    return K8sServerInstanceInfo.builder()
        .name(k8sPod.getName())
        .namespace(k8sPod.getNamespace())
        .releaseName(k8sPod.getReleaseName())
        .podIP(k8sPod.getPodIP())
        .containerList(k8sPod.getContainerList())
        .blueGreenColor(k8sPod.getColor())
        .build();
  }
}
