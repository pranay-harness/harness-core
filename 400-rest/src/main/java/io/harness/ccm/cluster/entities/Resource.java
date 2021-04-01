package io.harness.ccm.cluster.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class Resource {
  private Double cpuUnits;
  private Double memoryMb;
}
