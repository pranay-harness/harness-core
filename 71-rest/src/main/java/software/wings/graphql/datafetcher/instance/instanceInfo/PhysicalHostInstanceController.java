package software.wings.graphql.datafetcher.instance.instanceInfo;

import com.google.inject.Inject;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.graphql.datafetcher.instance.InstanceControllerUtils;
import software.wings.graphql.schema.type.instance.QLInstanceType;
import software.wings.graphql.schema.type.instance.QLPhysicalHostInstance;

public class PhysicalHostInstanceController implements InstanceController<QLPhysicalHostInstance> {
  @Inject InstanceControllerUtils util;

  @Override
  public QLPhysicalHostInstance populateInstance(Instance instance) {
    PhysicalHostInstanceInfo info = (PhysicalHostInstanceInfo) instance.getInstanceInfo();
    return QLPhysicalHostInstance.builder()
        .id(instance.getUuid())
        .applicationId(instance.getAppId())
        .environmentId(instance.getEnvId())
        .serviceId(instance.getServiceId())
        .artifact(util.getQlArtifact(instance))
        .type(QLInstanceType.PHYSICAL_HOST_INSTANCE)
        .hostId(info.getHostId())
        .hostName(info.getHostName())
        .hostPublicDns(info.getHostPublicDns())
        .build();
  }
}
