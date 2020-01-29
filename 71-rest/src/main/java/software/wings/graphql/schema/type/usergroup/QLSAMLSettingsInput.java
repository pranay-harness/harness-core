package software.wings.graphql.schema.type.usergroup;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSAMLSettingsKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLSAMLSettingsInput {
  String ssoProviderId;
  String groupName;
}
