package io.harness.cdng.artifact.steps;

import io.harness.cdng.artifact.bean.ArtifactConfig;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class ArtifactStepParameters {
  @NotNull ArtifactConfig artifact;
  ArtifactConfig artifactOverrideSet;
  ArtifactConfig artifactStageOverride;
}
