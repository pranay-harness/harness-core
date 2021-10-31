package software.wings.graphql.schema.mutation.artifact;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "SetArtifactCollectionEnabledInputKeys")
public class QLSetArtifactCollectionEnabledInput {
  String appId;
  String artifactStreamId;
  Boolean artifactCollectionEnabled;
  String clientMutationId;
}
