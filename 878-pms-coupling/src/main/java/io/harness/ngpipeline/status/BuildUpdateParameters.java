package io.harness.ngpipeline.status;

import io.harness.pms.sdk.core.steps.io.StepParameters;

public interface BuildUpdateParameters extends StepParameters {
  BuildUpdateType getBuildUpdateType();
}
