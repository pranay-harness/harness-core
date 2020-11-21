package software.wings.graphql.schema.type.secrets;

import io.harness.utils.RequestField;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSHCredentialUpdateKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLSSHCredentialUpdate {
  RequestField<String> name;
  QLSSHAuthenticationScheme authenticationScheme;
  RequestField<QLSSHAuthenticationInput> sshAuthentication;
  RequestField<QLKerberosAuthenticationInput> kerberosAuthentication;
  RequestField<QLUsageScope> usageScope;
}
