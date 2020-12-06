package software.wings.service.intfc.ownership;

public interface OwnedByInfrastructureProvisioner {
  /**
   * Prune if belongs to infrastructure mapping.
   *
   * @param appId the app id
   * @param infrastructureProvisionerId the infrastructure provisioner id
   */
  void pruneByInfrastructureProvisioner(String appId, String infrastructureProvisionerId);
}
