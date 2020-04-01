package io.harness.ccm.cluster.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Resource {
  private Double cpuUnits;
  private Double memoryMb;
}