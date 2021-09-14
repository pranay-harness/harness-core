/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.artifacts;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;

@OwnedBy(HarnessTeam.PIPELINE)
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

  public ArtifactTaskExecutionResponse getImages(T attributesRequest) {
    throw new InvalidRequestException("Operation not supported");
  }
}
