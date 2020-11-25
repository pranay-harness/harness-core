package io.harness.delegate.task.artifacts.response;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ArtifactTaskExecutionResponse {
  @Singular List<ArtifactDelegateResponse> artifactDelegateResponses;
  boolean isArtifactServerValid;
  boolean isArtifactSourceValid;
}
