/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.artifact.mappers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactMetadataKeys;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.EcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;
import io.harness.ngpipeline.artifact.bean.ArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.DockerArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.EcrArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.GcrArtifactOutcome;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDC)
public class ArtifactResponseToOutcomeMapper {
  private final String IMAGE_PULL_SECRET = "<+imagePullSecret.";
  public ArtifactOutcome toArtifactOutcome(
      ArtifactConfig artifactConfig, ArtifactDelegateResponse artifactDelegateResponse, boolean useDelegateResponse) {
    switch (artifactConfig.getSourceType()) {
      case DOCKER_REGISTRY:
        DockerHubArtifactConfig dockerConfig = (DockerHubArtifactConfig) artifactConfig;
        DockerArtifactDelegateResponse dockerDelegateResponse =
            (DockerArtifactDelegateResponse) artifactDelegateResponse;
        return getDockerArtifactOutcome(dockerConfig, dockerDelegateResponse, useDelegateResponse);
      case GCR:
        GcrArtifactConfig gcrArtifactConfig = (GcrArtifactConfig) artifactConfig;
        GcrArtifactDelegateResponse gcrArtifactDelegateResponse =
            (GcrArtifactDelegateResponse) artifactDelegateResponse;
        return getGcrArtifactOutcome(gcrArtifactConfig, gcrArtifactDelegateResponse, useDelegateResponse);
      case ECR:
        EcrArtifactConfig ecrArtifactConfig = (EcrArtifactConfig) artifactConfig;
        EcrArtifactDelegateResponse ecrArtifactDelegateResponse =
            (EcrArtifactDelegateResponse) artifactDelegateResponse;
        return getEcrArtifactOutcome(ecrArtifactConfig, ecrArtifactDelegateResponse, useDelegateResponse);
      default:
        throw new UnsupportedOperationException(
            String.format("Unknown Artifact Config type: [%s]", artifactConfig.getSourceType()));
    }
  }

  private DockerArtifactOutcome getDockerArtifactOutcome(DockerHubArtifactConfig dockerConfig,
      DockerArtifactDelegateResponse dockerDelegateResponse, boolean useDelegateResponse) {
    return DockerArtifactOutcome.builder()
        .image(getImageValue(dockerDelegateResponse))
        .connectorRef(dockerConfig.getConnectorRef().getValue())
        .imagePath(dockerConfig.getImagePath().getValue())
        .tag(useDelegateResponse ? dockerDelegateResponse.getTag()
                                 : (dockerConfig.getTag() != null ? dockerConfig.getTag().getValue() : null))
        .tagRegex(dockerConfig.getTagRegex() != null ? dockerConfig.getTagRegex().getValue() : null)
        .identifier(dockerConfig.getIdentifier())
        .type(ArtifactSourceType.DOCKER_REGISTRY.getDisplayName())
        .primaryArtifact(dockerConfig.isPrimaryArtifact())
        .imagePullSecret(IMAGE_PULL_SECRET + ArtifactUtils.getArtifactKey(dockerConfig) + ">")
        .build();
  }

  private GcrArtifactOutcome getGcrArtifactOutcome(GcrArtifactConfig gcrArtifactConfig,
      GcrArtifactDelegateResponse gcrArtifactDelegateResponse, boolean useDelegateResponse) {
    return GcrArtifactOutcome.builder()
        .image(getImageValue(gcrArtifactDelegateResponse))
        .connectorRef(gcrArtifactConfig.getConnectorRef().getValue())
        .imagePath(gcrArtifactConfig.getImagePath().getValue())
        .registryHostname(gcrArtifactConfig.getRegistryHostname().getValue())
        .tag(useDelegateResponse ? gcrArtifactDelegateResponse.getTag()
                                 : (gcrArtifactConfig.getTag() != null ? gcrArtifactConfig.getTag().getValue() : null))
        .tagRegex(gcrArtifactConfig.getTagRegex() != null ? gcrArtifactConfig.getTagRegex().getValue() : null)
        .identifier(gcrArtifactConfig.getIdentifier())
        .type(ArtifactSourceType.GCR.getDisplayName())
        .primaryArtifact(gcrArtifactConfig.isPrimaryArtifact())
        .imagePullSecret(IMAGE_PULL_SECRET + ArtifactUtils.getArtifactKey(gcrArtifactConfig) + ">")
        .build();
  }

  private EcrArtifactOutcome getEcrArtifactOutcome(EcrArtifactConfig ecrArtifactConfig,
      EcrArtifactDelegateResponse ecrArtifactDelegateResponse, boolean useDelegateResponse) {
    return EcrArtifactOutcome.builder()
        .image(getImageValue(ecrArtifactDelegateResponse))
        .connectorRef(ecrArtifactConfig.getConnectorRef().getValue())
        .imagePath(ecrArtifactConfig.getImagePath().getValue())
        .region(ecrArtifactConfig.getRegion().getValue())
        .tag(useDelegateResponse ? ecrArtifactDelegateResponse.getTag()
                                 : (ecrArtifactConfig.getTag() != null ? ecrArtifactConfig.getTag().getValue() : null))
        .tagRegex(ecrArtifactConfig.getTagRegex() != null ? ecrArtifactConfig.getTagRegex().getValue() : null)
        .identifier(ecrArtifactConfig.getIdentifier())
        .type(ArtifactSourceType.ECR.getDisplayName())
        .primaryArtifact(ecrArtifactConfig.isPrimaryArtifact())
        .imagePullSecret(IMAGE_PULL_SECRET + ArtifactUtils.getArtifactKey(ecrArtifactConfig) + ">")
        .build();
  }

  private String getImageValue(ArtifactDelegateResponse artifactDelegateResponse) {
    if (artifactDelegateResponse == null || artifactDelegateResponse.getBuildDetails() == null) {
      return null;
    }
    return EmptyPredicate.isNotEmpty(artifactDelegateResponse.getBuildDetails().getMetadata())
        ? artifactDelegateResponse.getBuildDetails().getMetadata().get(ArtifactMetadataKeys.IMAGE)
        : null;
  }
}
