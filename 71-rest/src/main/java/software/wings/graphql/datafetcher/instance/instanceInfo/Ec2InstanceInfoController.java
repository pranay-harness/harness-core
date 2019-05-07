package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;
import software.wings.graphql.schema.type.instance.info.QLEc2InstanceInfo;

public class Ec2InstanceInfoController implements InstanceInfoController<Ec2InstanceInfo> {
  @Override
  public void populateInstanceInfo(Ec2InstanceInfo info, QLInstanceBuilder builder) {
    builder.ec2InstanceInfo(QLEc2InstanceInfo.builder()
                                .hostPublicDns(info.getHostPublicDns())
                                .hostId(info.getHostId())
                                .hostName(info.getHostName())
                                .build());
  }
}
