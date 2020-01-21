package software.wings.graphql.schema.mutation.userGroup.input;

import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.permissions.QLUserGroupPermissions;
import software.wings.graphql.schema.type.usergroup.QLNotificationSettings;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUpdateUserGroupInputKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLUpdateUserGroupInput {
  String requestId;
  RequestField<String> name;
  RequestField<String> description;
  String userGroupId;
  RequestField<List<String>> userIds;
  RequestField<QLUserGroupPermissions> permissions;
  RequestField<QLNotificationSettings> notificationSettings;
}