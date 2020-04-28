package io.harness.artifact;

import io.harness.perpetualtask.PerpetualTaskClientParams;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtifactCollectionPTaskClientParams implements PerpetualTaskClientParams {
  String artifactStreamId;
}
