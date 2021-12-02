package io.harness.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.perpetualtask.PerpetualTaskClientParams;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "ArtifactCollectionPTaskClientParamsKeys")
public class ArtifactCollectionPTaskClientParams implements PerpetualTaskClientParams {
  String artifactStreamId;
}
