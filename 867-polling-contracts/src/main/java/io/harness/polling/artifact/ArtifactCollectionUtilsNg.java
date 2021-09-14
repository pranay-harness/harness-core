/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.polling.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class ArtifactCollectionUtilsNg {
  // TODO: Move to a common package which ng manager and delegate can depend on
  public static String getArtifactKey(ArtifactDelegateResponse artifactDelegateResponse) {
    switch (artifactDelegateResponse.getSourceType()) {
      case DOCKER_REGISTRY:
        return ((DockerArtifactDelegateResponse) artifactDelegateResponse).getTag();
      case ECR:
        return ((EcrArtifactDelegateResponse) artifactDelegateResponse).getTag();
      case GCR:
        return ((GcrArtifactDelegateResponse) artifactDelegateResponse).getTag();
      default:
        throw new InvalidRequestException(
            String.format("Source type %s not supported", artifactDelegateResponse.getSourceType()));
    }
  }
}
