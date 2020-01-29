package software.wings.graphql.schema.type.usergroup;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSOSettingInputKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLSSOSettingInput {
  QLLDAPSettingsInput ldapSettings;
  QLSAMLSettingsInput samlSettings;
}
