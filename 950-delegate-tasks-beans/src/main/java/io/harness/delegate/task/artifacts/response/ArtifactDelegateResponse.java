package io.harness.delegate.task.artifacts.response;

import io.harness.delegate.task.artifacts.ArtifactSourceType;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Interface for getting Dto response to create concrete Artifact.
 */
@Getter
@AllArgsConstructor
public abstract class ArtifactDelegateResponse {
  ArtifactBuildDetailsNG buildDetails;
  /** Artifact Source type.*/
  ArtifactSourceType sourceType;
}
