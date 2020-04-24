package software.wings.graphql.schema.mutation.execution.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLArtifactIdInputKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
public class QLArtifactIdInput {
  String artifactId;
}
