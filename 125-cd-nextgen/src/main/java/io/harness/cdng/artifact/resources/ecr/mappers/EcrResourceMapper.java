package io.harness.cdng.artifact.resources.ecr.mappers;

import com.google.inject.Singleton;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrBuildDetailsDTO;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrResponseDTO;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class EcrResourceMapper {
    public EcrResponseDTO toEcrResponse(List<EcrArtifactDelegateResponse> ecrArtifactDelegateResponseList) {
        List<EcrBuildDetailsDTO> detailsDTOList =
                ecrArtifactDelegateResponseList.stream()
                        .map(response -> toEcrBuildDetailsDTO(response.getBuildDetails(), response.getImagePath()))
                        .collect(Collectors.toList());
        return EcrResponseDTO.builder().buildDetailsList(detailsDTOList).build();
    }

    public EcrBuildDetailsDTO toEcrBuildDetailsDTO(ArtifactBuildDetailsNG artifactBuildDetailsNG, String imagePath) {
        return EcrBuildDetailsDTO.builder()
                .tag(artifactBuildDetailsNG.getNumber())
                .buildUrl(artifactBuildDetailsNG.getBuildUrl())
                .labels(artifactBuildDetailsNG.getLabelsMap())
                .metadata(artifactBuildDetailsNG.getMetadata())
                .imagePath(imagePath)
                .build();
    }
}
