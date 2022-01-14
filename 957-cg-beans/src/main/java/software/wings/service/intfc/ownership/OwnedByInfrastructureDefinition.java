package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface OwnedByInfrastructureDefinition {
  /**
   * Prune if belongs to infrastructure definition.
   *
   * @param appId the app id
   * @param infraDefinitionId the infrastructure definition id
   */
  void pruneByInfrastructureDefinition(String appId, String infraDefinitionId);
}
