package io.harness.yaml.core;

import io.harness.yaml.core.intfc.ArtifactStream;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

/**
 * Base class for defining Artifacts
 */
@Value
@Builder
public class Artifact {
  @NotNull String identifier;
  @NotNull ArtifactStream artifactStream;
}
