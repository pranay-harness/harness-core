/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
