package software.wings.service.intfc;

public interface OwnedByHost {
  /**
   * Prune if belongs to pipeline.
   *
   * @param appId the app id
   * @param hostId the host id
   */
  void pruneByHost(String appId, String hostId);
}
