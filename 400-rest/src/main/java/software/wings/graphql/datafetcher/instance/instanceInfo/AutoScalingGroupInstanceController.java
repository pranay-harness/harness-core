package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.graphql.datafetcher.instance.InstanceControllerUtils;
import software.wings.graphql.schema.type.instance.QLAutoScalingGroupInstance;
import software.wings.graphql.schema.type.instance.QLInstanceType;

import com.google.inject.Inject;

public class AutoScalingGroupInstanceController implements InstanceController<QLAutoScalingGroupInstance> {
  @Inject InstanceControllerUtils util;

  @Override
  public QLAutoScalingGroupInstance populateInstance(Instance instance) {
    AutoScalingGroupInstanceInfo info = (AutoScalingGroupInstanceInfo) instance.getInstanceInfo();
    return QLAutoScalingGroupInstance.builder()
        .id(instance.getUuid())
        .applicationId(instance.getAppId())
        .environmentId(instance.getEnvId())
        .serviceId(instance.getServiceId())
        .artifact(util.getQlArtifact(instance))
        .type(QLInstanceType.AUTOSCALING_GROUP_INSTANCE)
        .autoScalingGroupName(info.getAutoScalingGroupName())
        .hostPublicDns(info.getHostPublicDns())
        .hostId(info.getHostId())
        .hostName(info.getHostName())
        .build();
  }
}
