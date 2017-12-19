package software.wings.service.intfc;

public interface OwnedByService {
  /**
   * Prune if belongs to environment.
   *
   * @param appId the app id
   * @param serviceId the service id
   */
  void pruneByService(String appId, String serviceId);
}
