package io.harness.cdng.artifact.resources.ecr.services;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrBuildDetailsDTO;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrRequestDTO;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrResponseDTO;

public interface EcrResourceService {
    EcrResponseDTO getBuildDetails(IdentifierRef ecrConnectorRef, String imagePath, String registryHostname,
                                   String orgIdentifier, String projectIdentifier);

    EcrBuildDetailsDTO getSuccessfulBuild(IdentifierRef dockerConnectorRef, String imagePath,
                                          EcrRequestDTO dockerRequestDTO, String orgIdentifier, String projectIdentifier);

    boolean validateArtifactServer(IdentifierRef ecrConnectorRef, String imagePath, String orgIdentifier,
                                   String projectIdentifier, String registryHostname);

    boolean validateArtifactSource(String imagePath, IdentifierRef ecrConnectorRef, String registryHostname,
                                   String orgIdentifier, String projectIdentifier);
}
