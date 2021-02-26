package software.wings.service.intfc.ownership;

public interface OwnedByInfrastructureDefinition {
  /**
   * Prune if belongs to infrastructure definition.
   *
   * @param appId the app id
   * @param infraDefinitionId the infrastructure definition id
   */
  void pruneByInfrastructureDefinition(String appId, String infraDefinitionId);
}
