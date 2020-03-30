package software.wings.sm;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.deployment.InstanceDetails.InstanceType.K8s;

import io.harness.deployment.InstanceDetails;
import lombok.experimental.UtilityClass;
import software.wings.cloudprovider.ContainerInfo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

@UtilityClass
public final class HelmStateHelper {
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
