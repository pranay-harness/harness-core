package io.harness.delegate.task.artifacts.response;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ArtifactTaskExecutionResponse {
  @Singular List<ArtifactDelegateResponse> artifactDelegateResponses;
  boolean isArtifactServerValid;
  boolean isArtifactSourceValid;
}
