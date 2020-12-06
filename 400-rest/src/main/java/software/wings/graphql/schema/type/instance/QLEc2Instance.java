package software.wings.graphql.schema.type.instance;

import software.wings.graphql.schema.type.artifact.QLArtifact;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class QLEc2Instance extends QLAbstractEc2Instance {
  @Builder
  public QLEc2Instance(String hostId, String hostName, String hostPublicDns, String id, QLInstanceType type,
      String environmentId, String applicationId, String serviceId, QLArtifact artifact, String autoScalingGroupName,
      String deploymentId) {
    super(hostId, hostName, hostPublicDns, id, type, environmentId, applicationId, serviceId, artifact);
  }
}
