package io.harness.ngpipeline.pipeline.executions.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface WithArtifactSummary {
  @JsonIgnore ArtifactSummary getArtifactSummary();
}
