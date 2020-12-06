package software.wings.graphql.schema.mutation.application.payload;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLDeleteApplicationResultKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLDeleteApplicationPayload implements QLMutationPayload {
  private String clientMutationId;
}
