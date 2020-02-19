package io.harness.config;

import lombok.Value;

@Value
public class PipelineConfig {
  private boolean enabled;
  private Integer envStateTimeout;
  private Integer approvalStateTimeout;

  public PipelineConfig() {
    this.enabled = false;
    this.envStateTimeout = -1;
    this.approvalStateTimeout = -1;
  }
}
