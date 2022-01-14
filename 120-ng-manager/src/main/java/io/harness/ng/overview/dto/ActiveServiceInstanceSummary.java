package io.harness.ng.overview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.models.dashboard.InstanceCountDetailsByEnvTypeBase;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class ActiveServiceInstanceSummary {
  private double changeRate;
  private InstanceCountDetailsByEnvTypeBase countDetails;
}