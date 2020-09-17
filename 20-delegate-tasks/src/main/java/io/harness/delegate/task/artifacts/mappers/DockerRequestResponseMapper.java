package io.harness.delegate.task.artifacts.mappers;

import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateResponse;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UtilityClass
public class DockerRequestResponseMapper {
  public DockerInternalConfig toDockerInternalConfig(DockerArtifactDelegateRequest request) {
    String password = "";
    String username = "";
    if (request.getDockerConnectorDTO().getAuth() != null) {
      DockerUserNamePasswordDTO credentials =
          (DockerUserNamePasswordDTO) request.getDockerConnectorDTO().getAuth().getCredentials();
      if (credentials.getPasswordRef() != null) {
        password = EmptyPredicate.isNotEmpty(credentials.getPasswordRef().getDecryptedValue())
            ? new String(credentials.getPasswordRef().getDecryptedValue())
            : null;
      }
      username = credentials.getUsername();
    }
    return DockerInternalConfig.builder()
        .dockerRegistryUrl(request.getDockerConnectorDTO().getDockerRegistryUrl())
        .username(username)
        .password(password)
        .build();
  }

  public DockerArtifactDelegateResponse toDockerResponse(
      BuildDetailsInternal buildDetailsInternal, DockerArtifactDelegateRequest request) {
    return DockerArtifactDelegateResponse.builder()
        .buildDetails(ArtifactBuildDetailsMapper.toBuildDetailsNG(buildDetailsInternal))
        .imagePath(request.getImagePath())
        .tag(buildDetailsInternal.getNumber())
        .sourceType(ArtifactSourceType.DOCKER_HUB)
        .build();
  }

  public List<DockerArtifactDelegateResponse> toDockerResponse(
      List<Map<String, String>> labelsList, DockerArtifactDelegateRequest request) {
    return IntStream.range(0, request.getTagsList().size())
        .mapToObj(i
            -> DockerArtifactDelegateResponse.builder()
                   .buildDetails(
                       ArtifactBuildDetailsMapper.toBuildDetailsNG(labelsList.get(i), request.getTagsList().get(i)))
                   .imagePath(request.getImagePath())
                   .sourceType(ArtifactSourceType.DOCKER_HUB)
                   .build())
        .collect(Collectors.toList());
  }
}
