package software.wings.graphql.schema.mutation.artifact;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "ArtifactCleanupInputKeys")
public class ArtifactCleanupInput {
  private String artifactStreamId;
}
