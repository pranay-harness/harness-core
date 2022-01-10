/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.deployment.InstanceDetails.InstanceType.K8s;

import io.harness.container.ContainerInfo;
import io.harness.deployment.InstanceDetails;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class ContainerHelper {
  @Nonnull
  public static List<InstanceDetails> generateInstanceDetails(List<ContainerInfo> containerInfos) {
    if (isNotEmpty(containerInfos)) {
      return containerInfos.stream()
          .filter(Objects::nonNull)
          .map(containerInfo
              -> InstanceDetails.builder()
                     .hostName(containerInfo.getHostName())
                     .instanceType(K8s)
                     .workloadName(containerInfo.getWorkloadName())
                     .newInstance(containerInfo.isNewContainer())
                     .k8s(InstanceDetails.K8s.builder()
                              .podName(containerInfo.getPodName())
                              .dockerId(containerInfo.getContainerId())
                              .ip(containerInfo.getIp())
                              .build())
                     .build())
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}
