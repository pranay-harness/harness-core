package software.wings.graphql.schema.mutation.artifact;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "ArtifactCleanupInputKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class ArtifactCleanupInput {
  private String artifactStreamId;
}
