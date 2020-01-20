package software.wings.graphql.schema.mutation.userGroup.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.permissions.QLUserGroupPermissions;
import software.wings.graphql.schema.type.usergroup.QLNotificationSettings;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateUserGroupInputKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLCreateUserGroupInput implements QLObject {
  String name;
  String description;
  QLUserGroupPermissions permissions;
  QLNotificationSettings notificationSettings;
  String requestId;
}
