package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface OwnedByApplication {
  /**
   * Prune if belongs to application.
   *
   * @param appId the app id
   */
  void pruneByApplication(String appId);
}
