package io.harness.cdng.artifact.resources.ecr.service;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrBuildDetailsDTO;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrRequestDTO;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrResponseDTO;

public interface EcrResourceService {
  EcrResponseDTO getBuildDetails(
      IdentifierRef ecrConnectorRef, String imagePath, String region, String orgIdentifier, String projectIdentifier);

  EcrBuildDetailsDTO getSuccessfulBuild(IdentifierRef dockerConnectorRef, String imagePath,
      EcrRequestDTO dockerRequestDTO, String orgIdentifier, String projectIdentifier);

  boolean validateArtifactServer(
      IdentifierRef ecrConnectorRef, String imagePath, String orgIdentifier, String projectIdentifier, String region);

  boolean validateArtifactSource(
      String imagePath, IdentifierRef ecrConnectorRef, String region, String orgIdentifier, String projectIdentifier);
}
