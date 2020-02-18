package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSHCredentialInputKeys")
public class QLSSHCredentialInput implements QLObject {
  String name;
  QLSSHAuthenticationScheme authenticationScheme;
  QLSSHAuthenticationInput sshAuthentication;
  QLKerberosAuthenticationInput kerberosAuthentication;
  private QLUsageScope usageScope;
}
