package io.harness.service.deploymentsummary;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.DeploymentSummaryDTO;

import java.util.Optional;

@OwnedBy(HarnessTeam.DX)
public interface DeploymentSummaryService {
  Optional<DeploymentSummaryDTO> getByDeploymentSummaryId(String deploymentSummaryId);
}
