package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.graphql.datafetcher.instance.InstanceControllerUtils;
import software.wings.graphql.schema.type.instance.QLInstanceType;
import software.wings.graphql.schema.type.instance.QLK8SPodInstance;
import software.wings.graphql.schema.type.instance.QLK8sContainer;

import com.google.inject.Inject;
import java.util.stream.Collectors;

public class K8SPodController implements InstanceController<QLK8SPodInstance> {
  @Inject InstanceControllerUtils util;

  @Override
  public QLK8SPodInstance populateInstance(Instance instance) {
    K8sPodInfo info = (K8sPodInfo) instance.getInstanceInfo();
    return QLK8SPodInstance.builder()
        .id(instance.getUuid())
        .applicationId(instance.getAppId())
        .environmentId(instance.getEnvId())
        .serviceId(instance.getServiceId())
        .artifact(util.getQlArtifact(instance))
        .type(QLInstanceType.KUBERNETES_CONTAINER_INSTANCE)
        .clusterName(info.getClusterName())
        .ip(info.getIp())
        .podName(info.getPodName())
        .namespace(info.getNamespace())
        .releaseName(info.getReleaseName())
        .containers(info.getContainers()
                        .stream()
                        .map(c
                            -> QLK8sContainer.builder()
                                   .containerId(c.getContainerId())
                                   .image(c.getImage())
                                   .name(c.getName())
                                   .build())
                        .collect(Collectors.toList()))

        .build();
  }
}
