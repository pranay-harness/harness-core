/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.artifact.resources.gcr.service;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.resources.gcr.dtos.GcrBuildDetailsDTO;
import io.harness.cdng.artifact.resources.gcr.dtos.GcrRequestDTO;
import io.harness.cdng.artifact.resources.gcr.dtos.GcrResponseDTO;

public interface GcrResourceService {
  GcrResponseDTO getBuildDetails(IdentifierRef gcrConnectorRef, String imagePath, String registryHostname,
      String orgIdentifier, String projectIdentifier);

  GcrBuildDetailsDTO getSuccessfulBuild(IdentifierRef dockerConnectorRef, String imagePath,
      GcrRequestDTO dockerRequestDTO, String orgIdentifier, String projectIdentifier);

  boolean validateArtifactServer(IdentifierRef gcrConnectorRef, String imagePath, String orgIdentifier,
      String projectIdentifier, String registryHostname);

  boolean validateArtifactSource(String imagePath, IdentifierRef gcrConnectorRef, String registryHostname,
      String orgIdentifier, String projectIdentifier);
}
