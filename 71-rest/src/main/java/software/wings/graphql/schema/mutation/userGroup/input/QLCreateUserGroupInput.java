package software.wings.graphql.schema.mutation.userGroup.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.permissions.QLUserGroupPermissions;
import software.wings.graphql.schema.type.usergroup.QLNotificationSettings;
import software.wings.graphql.schema.type.usergroup.QLSSOSettingInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateUserGroupInputKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLCreateUserGroupInput implements QLMutationInput {
  String name;
  String description;
  QLUserGroupPermissions permissions;
  List<String> userIds;
  QLSSOSettingInput ssoSetting;
  QLNotificationSettings notificationSettings;
  String clientMutationId;
}
