package software.wings.graphql.schema.mutation.application;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLDeleteApplicationResultKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLDeleteApplicationResult implements QLObject {
  Boolean success;
}
