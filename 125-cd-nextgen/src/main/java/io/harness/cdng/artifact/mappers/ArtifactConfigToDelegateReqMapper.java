package io.harness.cdng.artifact.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.EcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactDelegateRequest;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class ArtifactConfigToDelegateReqMapper {
  public DockerArtifactDelegateRequest getDockerDelegateRequest(DockerHubArtifactConfig artifactConfig,
      DockerConnectorDTO connectorDTO, List<EncryptedDataDetail> encryptedDataDetails, String connectorRef) {
    // If both are empty, regex is latest among all docker artifacts.
    String tagRegex = artifactConfig.getTagRegex() != null ? artifactConfig.getTagRegex().getValue() : "";
    String tag = artifactConfig.getTag() != null ? artifactConfig.getTag().getValue() : "";
    if (EmptyPredicate.isEmpty(tag) && EmptyPredicate.isEmpty(tagRegex)) {
      tagRegex = "\\*";
    }
    return DockerArtifactDelegateRequest.builder()
        .imagePath(artifactConfig.getImagePath().getValue())
        .tag(tag)
        .tagRegex(tagRegex)
        .dockerConnectorDTO(connectorDTO)
        .encryptedDataDetails(encryptedDataDetails)
        .connectorRef(connectorRef)
        .sourceType(ArtifactSourceType.DOCKER_REGISTRY)
        .build();
  }

  public GcrArtifactDelegateRequest getGcrDelegateRequest(GcrArtifactConfig gcrArtifactConfig,
      GcpConnectorDTO gcpConnectorDTO, List<EncryptedDataDetail> encryptedDataDetails, String connectorRef) {
    // If both are empty, regex is latest among all gcr artifacts.
    String tagRegex = gcrArtifactConfig.getTagRegex() != null ? gcrArtifactConfig.getTagRegex().getValue() : "";
    String tag = gcrArtifactConfig.getTag() != null ? gcrArtifactConfig.getTag().getValue() : "";
    if (EmptyPredicate.isEmpty(tag) && EmptyPredicate.isEmpty(tagRegex)) {
      tagRegex = "\\*";
    }
    return GcrArtifactDelegateRequest.builder()
        .imagePath(gcrArtifactConfig.getImagePath().getValue())
        .tag(tag)
        .tagRegex(tagRegex)
        .registryHostname(gcrArtifactConfig.getRegistryHostname().getValue())
        .sourceType(ArtifactSourceType.GCR)
        .connectorRef(connectorRef)
        .gcpConnectorDTO(gcpConnectorDTO)
        .encryptedDataDetails(encryptedDataDetails)
        .build();
  }

  public EcrArtifactDelegateRequest getEcrDelegateRequest(EcrArtifactConfig ecrArtifactConfig,
      AwsConnectorDTO awsConnectorDTO, List<EncryptedDataDetail> encryptedDataDetails, String connectorRef) {
    // If both are empty, regex is latest among all ecr artifacts.
    String tagRegex = ecrArtifactConfig.getTagRegex() != null ? ecrArtifactConfig.getTagRegex().getValue() : "";
    String tag = ecrArtifactConfig.getTag() != null ? ecrArtifactConfig.getTag().getValue() : "";
    if (EmptyPredicate.isEmpty(tag) && EmptyPredicate.isEmpty(tagRegex)) {
      tagRegex = "\\*";
    }
    return EcrArtifactDelegateRequest.builder()
        .imagePath(ecrArtifactConfig.getImagePath().getValue())
        .tag(tag)
        .tagRegex(tagRegex)
        .region(ecrArtifactConfig.getRegion().getValue())
        .sourceType(ArtifactSourceType.ECR)
        .awsConnectorDTO(awsConnectorDTO)
        .connectorRef(connectorRef)
        .encryptedDataDetails(encryptedDataDetails)
        .build();
  }
}
