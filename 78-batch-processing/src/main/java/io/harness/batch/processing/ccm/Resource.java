package io.harness.batch.processing.ccm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Resource {
  private Double cpu;
  private String cpuUnit;
  private String cpuFormat;

  private Double memory;
  private String memoryUnit;
  private String memoryFormat;
}
