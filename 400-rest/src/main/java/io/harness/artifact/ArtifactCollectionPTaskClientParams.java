package io.harness.artifact;

import io.harness.annotations.dev.OwnedBy;
import io.harness.perpetualtask.PerpetualTaskClientParams;
import lombok.Builder;
import lombok.Value;

import static io.harness.annotations.dev.HarnessTeam.CDC;

@OwnedBy(CDC)
@Value
@Builder
public class ArtifactCollectionPTaskClientParams implements PerpetualTaskClientParams {
  String artifactStreamId;
}
