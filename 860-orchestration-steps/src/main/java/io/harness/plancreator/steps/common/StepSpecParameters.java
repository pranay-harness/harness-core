package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface StepSpecParameters {
  default StepSpecParameters getViewJsonObject() {
    return this;
  }
}
