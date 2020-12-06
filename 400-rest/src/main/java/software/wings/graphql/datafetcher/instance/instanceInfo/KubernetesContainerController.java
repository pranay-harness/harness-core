package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.graphql.datafetcher.instance.InstanceControllerUtils;
import software.wings.graphql.schema.type.instance.QLInstanceType;
import software.wings.graphql.schema.type.instance.QLK8SPodInstance;

import com.google.inject.Inject;

public class KubernetesContainerController implements InstanceController<QLK8SPodInstance> {
  @Inject InstanceControllerUtils util;

  @Override
  public QLK8SPodInstance populateInstance(Instance instance) {
    KubernetesContainerInfo info = (KubernetesContainerInfo) instance.getInstanceInfo();

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
        .build();
  }
}
