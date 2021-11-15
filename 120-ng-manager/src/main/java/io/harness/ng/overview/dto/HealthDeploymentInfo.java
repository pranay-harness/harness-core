package io.harness.ng.overview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class HealthDeploymentInfo {
  private TotalDeploymentInfo total;
  private io.harness.ng.overview.dto.DeploymentInfo success;
  private DeploymentInfo failure;
}
