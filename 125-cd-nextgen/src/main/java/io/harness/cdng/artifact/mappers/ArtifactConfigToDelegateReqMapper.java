package io.harness.cdng.artifact.mappers;

import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ArtifactConfigToDelegateReqMapper {
  public DockerArtifactDelegateRequest getDockerDelegateRequest(DockerHubArtifactConfig artifactConfig,
      DockerConnectorDTO connectorDTO, List<EncryptedDataDetail> encryptedDataDetails) {
    // If both are empty, regex is latest among all docker artifacts.
    String tagRegex = artifactConfig.getTagRegex() != null ? artifactConfig.getTagRegex().getValue() : "";
    String tag = artifactConfig.getTag() != null ? artifactConfig.getTag().getValue() : "";
    if (EmptyPredicate.isEmpty(tag) && EmptyPredicate.isEmpty(tagRegex)) {
      tagRegex = "*";
    }
    return DockerArtifactDelegateRequest.builder()
        .imagePath(artifactConfig.getImagePath().getValue())
        .tag(tag)
        .tagRegex(tagRegex)
        .dockerConnectorDTO(connectorDTO)
        .encryptedDataDetails(encryptedDataDetails)
        .sourceType(ArtifactSourceType.DOCKER_HUB)
        .build();
  }
}
