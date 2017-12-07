package software.wings.service.intfc;

public interface OwnedByApplication {
  /**
   * Prune if belongs to application.
   *
   * @param appId the app id
   */
  void pruneByApplication(String appId);
}
