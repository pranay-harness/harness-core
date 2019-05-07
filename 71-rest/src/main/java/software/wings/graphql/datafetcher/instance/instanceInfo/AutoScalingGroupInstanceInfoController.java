package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;
import software.wings.graphql.schema.type.instance.info.QLEc2InstanceInfo;

public class AutoScalingGroupInstanceInfoController implements InstanceInfoController<AutoScalingGroupInstanceInfo> {
  @Override
  public void populateInstanceInfo(AutoScalingGroupInstanceInfo info, QLInstanceBuilder builder) {
    builder.ec2InstanceInfo(QLEc2InstanceInfo.builder()
                                .autoScalingGroupName(info.getAutoScalingGroupName())
                                .hostPublicDns(info.getHostPublicDns())
                                .hostId(info.getHostId())
                                .hostName(info.getHostName())
                                .build());
  }
}
