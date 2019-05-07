package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.instance.info.QLEc2InstanceInfo;
import software.wings.graphql.schema.type.instance.info.QLEcsContainerInfo;
import software.wings.graphql.schema.type.instance.info.QLInstanceType;
import software.wings.graphql.schema.type.instance.info.QLK8SPodInfo;
import software.wings.graphql.schema.type.instance.info.QLPcfInstanceInfo;
import software.wings.graphql.schema.type.instance.info.QLPhysicalHostInstanceInfo;

@Value
@Builder
public class QLInstance {
  private String id;
  private QLInstanceType type;
  private String envId;
  private String appId;
  private String serviceId;
  private String lastArtifactId;
  private QLPhysicalHostInstanceInfo physicalHostInstanceInfo;
  private QLEc2InstanceInfo ec2InstanceInfo;
  private QLK8SPodInfo k8sPodInfo;
  private QLEcsContainerInfo ecsContainerInfo;
  private QLPcfInstanceInfo pcfInstanceInfo;
}
