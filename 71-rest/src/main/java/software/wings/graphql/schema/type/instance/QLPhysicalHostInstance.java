package software.wings.graphql.schema.type.instance;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import software.wings.graphql.schema.type.artifact.QLArtifact;

/**
 *
 * @author rktummala on 09/05/17
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class QLPhysicalHostInstance extends QLHostInstance {
  @Builder
  public QLPhysicalHostInstance(String hostId, String hostName, String hostPublicDns, String id, QLInstanceType type,
      String environmentId, String applicationId, String serviceId, QLArtifact artifact) {
    super(hostId, hostName, hostPublicDns, id, type, environmentId, applicationId, serviceId, artifact);
  }
}
