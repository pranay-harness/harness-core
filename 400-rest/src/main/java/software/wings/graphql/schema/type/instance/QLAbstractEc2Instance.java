package software.wings.graphql.schema.type.instance;

import software.wings.graphql.schema.type.artifact.QLArtifact;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 08/25/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class QLAbstractEc2Instance extends QLHostInstance {
  public QLAbstractEc2Instance(String hostId, String hostName, String hostPublicDns, String id, QLInstanceType type,
      String environmentId, String applicationId, String serviceId, QLArtifact artifact) {
    super(hostId, hostName, hostPublicDns, id, type, environmentId, applicationId, serviceId, artifact);
  }
}
