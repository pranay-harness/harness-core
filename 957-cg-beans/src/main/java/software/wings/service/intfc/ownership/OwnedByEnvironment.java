package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface OwnedByEnvironment {
  /**
   * Prune if belongs to environment.
   *
   * @param appId the app id
   * @param envId the environment id
   */
  void pruneByEnvironment(String appId, String envId);
}
