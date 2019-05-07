package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;
import software.wings.graphql.schema.type.instance.info.QLK8SPodInfo;
import software.wings.graphql.schema.type.instance.info.QLK8sContainerInfo;

import java.util.stream.Collectors;

public class K8sPodInfoController implements InstanceInfoController<K8sPodInfo> {
  @Override
  public void populateInstanceInfo(K8sPodInfo info, QLInstanceBuilder builder) {
    builder.k8sPodInfo(QLK8SPodInfo.builder()
                           .clusterName(info.getClusterName())
                           .ip(info.getIp())
                           .podName(info.getPodName())
                           .namespace(info.getNamespace())
                           .releaseName(info.getReleaseName())
                           .containers(info.getContainers()
                                           .stream()
                                           .map(c
                                               -> QLK8sContainerInfo.builder()
                                                      .containerId(c.getContainerId())
                                                      .image(c.getImage())
                                                      .name(c.getName())
                                                      .build())
                                           .collect(Collectors.toList()))

                           .build());
  }
}
