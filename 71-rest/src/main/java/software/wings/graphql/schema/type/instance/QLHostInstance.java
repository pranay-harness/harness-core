package software.wings.graphql.schema.type.instance;

import software.wings.graphql.schema.type.artifact.QLArtifact;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author rktummala on 09/05/17
 */
@Data
@AllArgsConstructor
public abstract class QLHostInstance implements QLInstance, QLPhysicalHost {
  private String hostId;
  private String hostName;
  private String hostPublicDns;

  private String id;
  private QLInstanceType type;
  private String environmentId;
  private String applicationId;
  private String serviceId;
  private QLArtifact artifact;
}
