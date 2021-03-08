package software.wings.graphql.schema.type.usergroup;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLLDAPSettingsKeys")
@Scope(PermissionAttribute.ResourceType.USER)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLLDAPSettingsInput {
  String ssoProviderId;
  String groupName;
  String groupDN;
}
