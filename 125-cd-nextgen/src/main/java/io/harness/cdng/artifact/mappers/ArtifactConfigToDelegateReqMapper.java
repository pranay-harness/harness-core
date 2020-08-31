package io.harness.cdng.artifact.mappers;

import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class ArtifactConfigToDelegateReqMapper {
  public DockerArtifactDelegateRequest getDockerDelegateRequest(DockerHubArtifactConfig artifactConfig,
      DockerConnectorDTO connectorDTO, List<EncryptedDataDetail> encryptedDataDetails) {
    // If both are empty, regex is latest among all docker artifacts.
    String tagRegex = "";
    if (EmptyPredicate.isEmpty(artifactConfig.getTag()) && EmptyPredicate.isEmpty(artifactConfig.getTagRegex())) {
      tagRegex = "*";
    }
    return DockerArtifactDelegateRequest.builder()
        .imagePath(artifactConfig.getImagePath())
        .tag(artifactConfig.getTag())
        .tagRegex(tagRegex)
        .dockerConnectorDTO(connectorDTO)
        .encryptedDataDetails(encryptedDataDetails)
        .sourceType(ArtifactSourceType.DOCKER_HUB)
        .build();
  }
}
