package io.harness.cdng.artifact.resources.docker.mappers;

import io.harness.cdng.artifact.resources.docker.dtos.DockerBuildDetailsDTO;
import io.harness.cdng.artifact.resources.docker.dtos.DockerResponseDTO;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class DockerResourceMapper {
  public DockerResponseDTO toDockerResponse(List<DockerArtifactDelegateResponse> dockerArtifactDelegateResponseList) {
    List<DockerBuildDetailsDTO> detailsDTOList =
        dockerArtifactDelegateResponseList.stream()
            .map(response -> toDockerBuildDetailsDTO(response.getBuildDetails(), response.getImagePath()))
            .collect(Collectors.toList());
    return DockerResponseDTO.builder().buildDetailsList(detailsDTOList).build();
  }

  public DockerBuildDetailsDTO toDockerBuildDetailsDTO(
      ArtifactBuildDetailsNG artifactBuildDetailsNG, String imagePath) {
    return DockerBuildDetailsDTO.builder()
        .tag(artifactBuildDetailsNG.getNumber())
        .buildUrl(artifactBuildDetailsNG.getBuildUrl())
        .labels(artifactBuildDetailsNG.getLabelsMap())
        .metadata(artifactBuildDetailsNG.getMetadata())
        .imagePath(imagePath)
        .build();
  }
}
