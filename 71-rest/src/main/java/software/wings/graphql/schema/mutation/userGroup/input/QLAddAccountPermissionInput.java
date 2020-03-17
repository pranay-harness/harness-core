package software.wings.graphql.schema.mutation.userGroup.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.permissions.QLAccountPermissionType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAddAccountPermissionInputKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLAddAccountPermissionInput {
  private String clientMutationId;
  private String userGroupId;
  private QLAccountPermissionType accountPermission;
}
