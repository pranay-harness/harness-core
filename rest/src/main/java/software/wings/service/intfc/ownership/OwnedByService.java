package software.wings.service.intfc.ownership;

public interface OwnedByService {
  /**
   * Prune if belongs to servoce.
   *
   * @param appId the app id
   * @param serviceId the service id
   */
  void pruneByService(String appId, String serviceId);
}
