package io.harness.cdng.artifact.steps;

import io.harness.cdng.artifact.bean.ArtifactConfig;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("artifactStepParameters")
public class ArtifactStepParameters {
  @NotNull ArtifactConfig artifact;
  ArtifactConfig artifactOverrideSet;
  ArtifactConfig artifactStageOverride;
}
