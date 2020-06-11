package io.harness.cdng.manifest.state;

import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManifestStepParameters implements StepParameters {
  private ManifestListConfig manifestServiceSpec;
  private ManifestListConfig manifestStageOverride;
}