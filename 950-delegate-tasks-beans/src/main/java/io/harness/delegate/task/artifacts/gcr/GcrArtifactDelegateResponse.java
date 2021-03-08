package io.harness.delegate.task.artifacts.gcr;

import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class GcrArtifactDelegateResponse extends ArtifactDelegateResponse {
  /** Images in repos need to be referenced via a path */
  String imagePath;
  /** Tag refers to exact tag number */
  String tag;

  @Builder
  public GcrArtifactDelegateResponse(
      ArtifactBuildDetailsNG buildDetails, ArtifactSourceType sourceType, String imagePath, String tag) {
    super(buildDetails, sourceType);
    this.imagePath = imagePath;
    this.tag = tag;
  }
}
