package io.harness.config;

import io.harness.annotations.dev.OwnedBy;
import lombok.Value;

import static io.harness.annotations.dev.HarnessTeam.CDC;

@OwnedBy(CDC)
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
