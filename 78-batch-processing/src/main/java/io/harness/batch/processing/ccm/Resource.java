package io.harness.batch.processing.ccm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Resource {
  private Double cpuUnits;
  private Double memoryMb;
}
