package io.harness.delegate.task.artifacts.response;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@OwnedBy(PIPELINE)
public class ArtifactTaskExecutionResponse {
  @Singular List<ArtifactDelegateResponse> artifactDelegateResponses;
  boolean isArtifactServerValid;
  boolean isArtifactSourceValid;
  List<String> artifactImages;
}
