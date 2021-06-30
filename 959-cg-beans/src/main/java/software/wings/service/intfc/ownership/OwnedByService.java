package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface OwnedByService {
  /**
   * Prune if belongs to servoce.
   *
   * @param appId the app id
   * @param serviceId the service id
   */
  void pruneByService(String appId, String serviceId);
}
