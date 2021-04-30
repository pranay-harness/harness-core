package io.harness.delegate.task.artifacts.mappers;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateResponse;

import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class EcrRequestResponseMapper {
  public EcrArtifactDelegateResponse toEcrResponse(
      BuildDetailsInternal buildDetailsInternal, EcrArtifactDelegateRequest request) {
    return EcrArtifactDelegateResponse.builder()
        .buildDetails(ArtifactBuildDetailsMapper.toBuildDetailsNG(buildDetailsInternal))
        .imagePath(request.getImagePath())
        .tag(buildDetailsInternal.getNumber())
        .sourceType(ArtifactSourceType.ECR)
        .build();
  }
}
