package io.harness.cdng.artifact.steps;

import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class ArtifactStepParameters implements StepParameters {
  @NotNull ArtifactConfigWrapper artifact;
  ArtifactConfigWrapper artifactOverrideSet;
  ArtifactConfigWrapper artifactStageOverride;
}
