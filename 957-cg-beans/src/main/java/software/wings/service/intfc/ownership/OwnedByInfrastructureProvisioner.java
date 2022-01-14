package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface OwnedByInfrastructureProvisioner {
  /**
   * Prune if belongs to infrastructure mapping.
   *
   * @param appId the app id
   * @param infrastructureProvisionerId the infrastructure provisioner id
   */
  void pruneByInfrastructureProvisioner(String appId, String infrastructureProvisionerId);
}
