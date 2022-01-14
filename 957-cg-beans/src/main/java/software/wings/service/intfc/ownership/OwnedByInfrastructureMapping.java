package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface OwnedByInfrastructureMapping {
  /**
   * Prune if belongs to infrastructure mapping.
   *
   * @param appId the app id
   * @param infrastructureMappingId the infrastructure mapping id
   */
  void pruneByInfrastructureMapping(String appId, String infrastructureMappingId);
}
