package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.graphql.datafetcher.instance.InstanceControllerUtils;
import software.wings.graphql.schema.type.instance.QLEcsContainerInstance;
import software.wings.graphql.schema.type.instance.QLInstanceType;

import com.google.inject.Inject;

public class EcsContainerController implements InstanceController<QLEcsContainerInstance> {
  @Inject InstanceControllerUtils util;

  @Override
  public QLEcsContainerInstance populateInstance(Instance instance) {
    EcsContainerInfo info = (EcsContainerInfo) instance.getInstanceInfo();

    return QLEcsContainerInstance.builder()
        .id(instance.getUuid())
        .applicationId(instance.getAppId())
        .environmentId(instance.getEnvId())
        .serviceId(instance.getServiceId())
        .artifact(util.getQlArtifact(instance))
        .type(QLInstanceType.ECS_CONTAINER_INSTANCE)
        .clusterName(info.getClusterName())
        .serviceName(info.getServiceName())
        .startedAt(info.getStartedAt())
        .startedBy(info.getStartedBy())
        .taskArn(info.getTaskArn())
        .taskDefinitionArn(info.getTaskDefinitionArn())
        .version(info.getVersion())
        .build();
  }
}
