package io.harness.delegate.task.artifacts;

import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;

public abstract class DelegateArtifactTaskHandler<T extends ArtifactSourceDelegateRequest> {
  public ArtifactTaskExecutionResponse getLastSuccessfulBuild(T attributesRequest) {
    throw new InvalidRequestException("Operation not supported");
  }

  public ArtifactTaskExecutionResponse getBuilds(T attributesRequest) {
    throw new InvalidRequestException("Operation not supported");
  }

  public ArtifactTaskExecutionResponse getLabels(T attributesRequest) {
    throw new InvalidRequestException("Operation not supported");
  }

  public ArtifactTaskExecutionResponse validateArtifactServer(T attributesRequest) {
    throw new InvalidRequestException("Operation not supported");
  }

  public ArtifactTaskExecutionResponse validateArtifactImage(T attributesRequest) {
    throw new InvalidRequestException("Operation not supported");
  }
}
