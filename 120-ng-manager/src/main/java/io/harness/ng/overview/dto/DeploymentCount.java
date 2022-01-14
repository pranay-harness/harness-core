package io.harness.ng.overview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
public class DeploymentCount {
  private long total;
  private long success;
  private long failure;
}
