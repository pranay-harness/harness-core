package io.harness.cdng.k8s;

import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sRollingStepParameters implements StepParameters {
  private int timeout;
  private boolean skipDryRun;
}
