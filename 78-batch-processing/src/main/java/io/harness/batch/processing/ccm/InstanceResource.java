package io.harness.batch.processing.ccm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceResource {
  private Double cpu;
  private Double memory;
}
