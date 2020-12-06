package software.wings.graphql.schema.type.usergroup;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSAMLSettingsKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLSAMLSettings implements QLLinkedSSOSetting {
  private String ssoProviderId;
  private String groupName;
}
