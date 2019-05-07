package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;
import software.wings.graphql.schema.type.instance.info.QLEcsContainerInfo;

public class EcsContainerInfoController implements InstanceInfoController<EcsContainerInfo> {
  @Override
  public void populateInstanceInfo(EcsContainerInfo info, QLInstanceBuilder builder) {
    builder.ecsContainerInfo(QLEcsContainerInfo.builder()
                                 .clusterName(info.getClusterName())
                                 .serviceName(info.getServiceName())
                                 .startedAt(GraphQLDateTimeScalar.convert(info.getStartedAt()))
                                 .startedBy(info.getStartedBy())
                                 .taskArn(info.getTaskArn())
                                 .taskDefinitionArn(info.getTaskDefinitionArn())
                                 .version(info.getVersion())
                                 .build());
  }
}
