package io.harness.state.io;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sRollingStepParameters implements StepParameters {
  private int timeout;
  private boolean skipDryRun;
}
