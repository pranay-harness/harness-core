package software.wings.graphql.schema.type.usergroup;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Scope(PermissionAttribute.ResourceType.USER)
public interface QLLinkedSSOSetting extends QLObject {
  String getSsoProviderId();
}
