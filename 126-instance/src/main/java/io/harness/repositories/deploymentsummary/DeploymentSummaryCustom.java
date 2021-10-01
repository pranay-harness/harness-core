package io.harness.repositories.deploymentsummary;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.DeploymentSummary;

import java.util.Optional;

@OwnedBy(HarnessTeam.DX)
public interface DeploymentSummaryCustom {
  Optional<DeploymentSummary> fetchNthRecordFromNow(int N, String instanceSyncKey);
  Optional<DeploymentSummary> getWithInfraMappingId(String infraMappingId);
}
