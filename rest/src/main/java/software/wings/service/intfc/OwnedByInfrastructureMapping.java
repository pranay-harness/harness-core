package software.wings.service.intfc;

public interface OwnedByInfrastructureMapping {
  /**
   * Prune if belongs to infrastructure mapping.
   *
   * @param appId the app id
   * @param infrastructureMappingId the infrastructure mapping id
   */
  void pruneByInfrastructureMapping(String appId, String infrastructureMappingId);
}
