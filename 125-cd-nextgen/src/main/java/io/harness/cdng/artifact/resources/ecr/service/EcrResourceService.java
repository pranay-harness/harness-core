/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.artifact.resources.ecr.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrBuildDetailsDTO;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrListImagesDTO;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrRequestDTO;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrResponseDTO;

@OwnedBy(PIPELINE)
public interface EcrResourceService {
  EcrResponseDTO getBuildDetails(
      IdentifierRef ecrConnectorRef, String imagePath, String region, String orgIdentifier, String projectIdentifier);

  EcrBuildDetailsDTO getSuccessfulBuild(IdentifierRef dockerConnectorRef, String imagePath,
      EcrRequestDTO dockerRequestDTO, String orgIdentifier, String projectIdentifier);

  boolean validateArtifactServer(
      IdentifierRef ecrConnectorRef, String imagePath, String orgIdentifier, String projectIdentifier, String region);

  boolean validateArtifactSource(
      String imagePath, IdentifierRef ecrConnectorRef, String region, String orgIdentifier, String projectIdentifier);

  EcrListImagesDTO getImages(
      IdentifierRef ecrConnectorRef, String region, String orgIdentifier, String projectIdentifier);
}
