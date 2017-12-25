package software.wings.service.intfc.ownership;

public interface OwnedByEnvironment {
  /**
   * Prune if belongs to environment.
   *
   * @param appId the app id
   * @param envId the environment id
   */
  void pruneByEnvironment(String appId, String envId);
}
