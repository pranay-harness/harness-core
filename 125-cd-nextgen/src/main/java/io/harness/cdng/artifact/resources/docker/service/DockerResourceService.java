/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.artifact.resources.docker.service;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.resources.docker.dtos.DockerBuildDetailsDTO;
import io.harness.cdng.artifact.resources.docker.dtos.DockerRequestDTO;
import io.harness.cdng.artifact.resources.docker.dtos.DockerResponseDTO;

public interface DockerResourceService {
  DockerResponseDTO getBuildDetails(
      IdentifierRef dockerConnectorRef, String imagePath, String orgIdentifier, String projectIdentifier);

  DockerResponseDTO getLabels(IdentifierRef dockerConnectorRef, String imagePath, DockerRequestDTO dockerRequestDTO,
      String orgIdentifier, String projectIdentifier);

  DockerBuildDetailsDTO getSuccessfulBuild(IdentifierRef dockerConnectorRef, String imagePath,
      DockerRequestDTO dockerRequestDTO, String orgIdentifier, String projectIdentifier);

  boolean validateArtifactServer(IdentifierRef dockerConnectorRef, String orgIdentifier, String projectIdentifier);

  // In case of docker source here is docker image.
  boolean validateArtifactSource(
      String imagePath, IdentifierRef dockerConnectorRef, String orgIdentifier, String projectIdentifier);
}
