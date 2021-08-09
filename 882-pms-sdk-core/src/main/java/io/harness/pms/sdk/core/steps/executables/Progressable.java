package io.harness.pms.sdk.core.steps.executables;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.tasks.ProgressData;

@OwnedBy(PIPELINE)
public interface Progressable<T extends StepParameters> {
  default ProgressData handleProgress(Ambiance ambiance, T stepParameters, ProgressData progressData) {
    return progressData;
  };
}
