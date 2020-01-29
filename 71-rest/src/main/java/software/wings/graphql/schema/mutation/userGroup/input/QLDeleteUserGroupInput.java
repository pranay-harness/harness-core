package software.wings.graphql.schema.mutation.userGroup.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLDeleteUserGroupInputKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLDeleteUserGroupInput implements QLMutationInput {
  String clientMutationId;
  String userGroupId;
}
