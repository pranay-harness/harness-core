/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states.azure.artifact;

import static java.lang.String.format;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.exception.InvalidRequestException;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.sm.states.azure.artifact.container.ACRArtifactStreamMapper;
import software.wings.sm.states.azure.artifact.container.ArtifactoryArtifactStreamMapper;
import software.wings.sm.states.azure.artifact.container.DockerArtifactStreamMapper;
import software.wings.utils.ArtifactType;

import java.util.Optional;

public abstract class ArtifactStreamMapper {
  protected ArtifactStreamAttributes artifactStreamAttributes;
  protected Artifact artifact;

  protected ArtifactStreamMapper(Artifact artifact, ArtifactStreamAttributes artifactStreamAttributes) {
    this.artifactStreamAttributes = artifactStreamAttributes;
    this.artifact = artifact;
  }

  public static ArtifactStreamMapper getArtifactStreamMapper(
      Artifact artifact, ArtifactStreamAttributes artifactStreamAttributes) {
    ArtifactStreamType artifactStreamType =
        ArtifactStreamType.valueOf(artifactStreamAttributes.getArtifactStreamType());
    ArtifactType artifactType = artifactStreamAttributes.getArtifactType();

    if (isDockerArtifactType(artifactType)) {
      return handleDockerArtifactTypes(artifact, artifactStreamAttributes, artifactStreamType);
    } else if (isArtifactStreamTypeSupportedByAzure(artifactStreamType)) {
      return new ArtifactStreamAttributesMapper(artifact, artifactStreamAttributes);
    } else {
      throw new InvalidRequestException(format(
          "Unsupported artifact stream type for non docker artifacts,  artifactStreamType: %s", artifactStreamType));
    }
  }

  private static ArtifactStreamMapper handleDockerArtifactTypes(
      Artifact artifact, ArtifactStreamAttributes artifactStreamAttributes, ArtifactStreamType artifactStreamType) {
    if (ArtifactStreamType.DOCKER == artifactStreamType) {
      return new DockerArtifactStreamMapper(artifact, artifactStreamAttributes);
    } else if (ArtifactStreamType.ARTIFACTORY == artifactStreamType) {
      return new ArtifactoryArtifactStreamMapper(artifact, artifactStreamAttributes);
    } else if (ArtifactStreamType.ACR == artifactStreamType) {
      return new ACRArtifactStreamMapper(artifact, artifactStreamAttributes);
    } else {
      throw new InvalidRequestException(
          format("Unsupported artifact stream type for docker artifacts,  artifactStreamType: %s", artifactStreamType));
    }
  }

  private static boolean isDockerArtifactType(ArtifactType artifactType) {
    return ArtifactType.DOCKER == artifactType;
  }

  private static boolean isArtifactStreamTypeSupportedByAzure(ArtifactStreamType streamType) {
    return ArtifactStreamType.ARTIFACTORY == streamType || ArtifactStreamType.NEXUS == streamType
        || ArtifactStreamType.JENKINS == streamType || ArtifactStreamType.BAMBOO == streamType
        || ArtifactStreamType.AMAZON_S3 == streamType || ArtifactStreamType.AZURE_ARTIFACTS == streamType;
  }

  public abstract ConnectorConfigDTO getConnectorDTO();
  public abstract AzureRegistryType getAzureRegistryType();
  public abstract boolean isDockerArtifactType();
  public abstract Optional<DecryptableEntity> getConnectorDTOAuthCredentials(ConnectorConfigDTO connectorConfigDTO);
  public abstract Optional<EncryptableSetting> getEncryptableSetting();

  public String getFullImageName() {
    return artifact.getMetadata().get("image");
  }

  public String getImageTag() {
    return artifact.getMetadata().get("tag");
  }

  public ArtifactStreamAttributes artifactStreamAttributes() {
    return artifactStreamAttributes;
  }
}
