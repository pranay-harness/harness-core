package software.wings.graphql.schema.mutation.userGroup.payload;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.permissions.QLGroupPermissions;
import software.wings.graphql.schema.type.usergroup.QLNotificationSettings;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateUserGroupPayloadKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLCreateUserGroupPayload implements QLObject {
  String name;
  String id;
  String description;
  QLGroupPermissions permissions;
  Boolean isSSOLinked;
  Boolean importedByScim;
  QLNotificationSettings notificationSettings;
  String requestId;
}
