package software.wings.graphql.schema.type.usergroup;

import io.harness.annotations.dev.HarnessModule;
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
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLLDAPSettings implements QLLinkedSSOSetting {
  private String ssoProviderId;
  private String groupName;
  private String groupDN;
}
