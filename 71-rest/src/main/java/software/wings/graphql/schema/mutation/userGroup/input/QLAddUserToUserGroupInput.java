package software.wings.graphql.schema.mutation.userGroup.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAddUserToUserGroupInputKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLAddUserToUserGroupInput {
  private String clientMutationId;
  private String userGroupId;
  private String userId;
}
