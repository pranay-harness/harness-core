package software.wings.graphql.schema.type.usergroup;

import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.permissions.QLGroupPermissions;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUserGroupKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLUserGroup implements QLObject {
  String name;
  String id;
  String description;
  QLGroupPermissions permissions;
  QLLinkedSSOSetting ssoSetting;
  Boolean isSSOLinked;
  Boolean importedByScim;
  QLNotificationSettings notificationSettings;
  String requestId;
}
