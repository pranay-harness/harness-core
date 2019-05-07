package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;
import software.wings.graphql.schema.type.instance.info.QLK8SPodInfo;

public class KubernetesContainerInfoController implements InstanceInfoController<KubernetesContainerInfo> {
  @Override
  public void populateInstanceInfo(KubernetesContainerInfo info, QLInstanceBuilder builder) {
    builder.k8sPodInfo(QLK8SPodInfo.builder()
                           .clusterName(info.getClusterName())
                           .ip(info.getIp())
                           .podName(info.getPodName())
                           .namespace(info.getNamespace())
                           .build());
  }
}
