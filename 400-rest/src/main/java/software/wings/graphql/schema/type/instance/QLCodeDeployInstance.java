package software.wings.graphql.schema.type.instance;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.artifact.QLArtifact;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 01/30/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLCodeDeployInstance extends QLAbstractEc2Instance {
  private String deploymentId;

  @Builder
  public QLCodeDeployInstance(String hostId, String hostName, String hostPublicDns, String id, QLInstanceType type,
      String environmentId, String applicationId, String serviceId, QLArtifact artifact, String deploymentId) {
    super(hostId, hostName, hostPublicDns, id, type, environmentId, applicationId, serviceId, artifact);
    this.deploymentId = deploymentId;
  }
}
