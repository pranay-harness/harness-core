package io.harness.yaml.core;

import io.harness.yaml.core.intfc.ArtifactStream;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

/**
 * Base class for defining Artifacts
 */
@Value
@Builder
public class Artifact {
  @NotNull String identifier;
  @NotNull ArtifactStream artifactStream;
}
