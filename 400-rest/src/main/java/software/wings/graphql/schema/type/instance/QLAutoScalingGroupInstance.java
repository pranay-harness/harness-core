package software.wings.graphql.schema.type.instance;

import software.wings.graphql.schema.type.artifact.QLArtifact;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class QLAutoScalingGroupInstance extends QLAbstractEc2Instance {
  private String autoScalingGroupName;

  @Builder
  public QLAutoScalingGroupInstance(String hostId, String hostName, String hostPublicDns, String id,
      QLInstanceType type, String environmentId, String applicationId, String serviceId, QLArtifact artifact,
      String autoScalingGroupName) {
    super(hostId, hostName, hostPublicDns, id, type, environmentId, applicationId, serviceId, artifact);
    this.autoScalingGroupName = autoScalingGroupName;
  }
}
