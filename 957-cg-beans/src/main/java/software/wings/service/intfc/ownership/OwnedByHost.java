package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface OwnedByHost {
  /**
   * Prune if belongs to pipeline.
   *
   * @param appId the app id
   * @param hostId the host id
   */
  void pruneByHost(String appId, String hostId);
}
