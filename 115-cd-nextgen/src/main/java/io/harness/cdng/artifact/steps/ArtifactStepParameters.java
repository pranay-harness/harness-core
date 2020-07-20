package io.harness.cdng.artifact.steps;

import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class ArtifactStepParameters implements StepParameters {
  @NotNull ArtifactConfig artifact;
  ArtifactConfig artifactOverrideSet;
  ArtifactConfig artifactStageOverride;
}
