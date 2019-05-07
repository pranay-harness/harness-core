package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;
import software.wings.graphql.schema.type.instance.info.QLPcfInstanceInfo;

public class PcfInstanceInfoController implements InstanceInfoController<PcfInstanceInfo> {
  @Override
  public void populateInstanceInfo(PcfInstanceInfo info, QLInstanceBuilder builder) {
    builder.pcfInstanceInfo(QLPcfInstanceInfo.builder()
                                .id(info.getId())
                                .instanceIndex(info.getInstanceIndex())
                                .organization(info.getOrganization())
                                .pcfApplicationGuid(info.getPcfApplicationGuid())
                                .pcfApplicationName(info.getPcfApplicationName())
                                .space(info.getSpace())
                                .build());
  }
}
