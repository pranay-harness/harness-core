package io.harness.artifact;

import io.harness.perpetualtask.PerpetualTaskClientParams;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class ArtifactCollectionPTaskClientParams implements PerpetualTaskClientParams {
  private String artifactStreamId;
}
