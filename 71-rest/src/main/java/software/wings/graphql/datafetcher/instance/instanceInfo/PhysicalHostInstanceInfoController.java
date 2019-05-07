package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;
import software.wings.graphql.schema.type.instance.info.QLPhysicalHostInstanceInfo;

public class PhysicalHostInstanceInfoController implements InstanceInfoController<PhysicalHostInstanceInfo> {
  public void populateInstanceInfo(PhysicalHostInstanceInfo info, QLInstanceBuilder builder) {
    builder.physicalHostInstanceInfo(QLPhysicalHostInstanceInfo.builder()
                                         .hostId(info.getHostId())
                                         .hostName(info.getHostName())
                                         .hostPublicDns(info.getHostPublicDns())
                                         .build());
  }
}
