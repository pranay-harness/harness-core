package software.wings.graphql.schema.mutation.application.payload;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLDeleteApplicationResultKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLDeleteApplicationPayload implements QLMutationPayload {
  private String clientMutationId;
}
