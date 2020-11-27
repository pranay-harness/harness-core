package io.harness.ngpipeline.status;

import io.harness.state.io.StepParameters;

public interface BuildUpdateParameters extends StepParameters {
  BuildUpdateType getBuildUpdateType();
}
