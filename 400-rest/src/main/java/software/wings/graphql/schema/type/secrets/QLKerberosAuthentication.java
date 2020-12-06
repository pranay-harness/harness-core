package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLKerberosAuthenticationKeys")
public class QLKerberosAuthentication implements QLSSHAuthenticationType {
  String principal;
  String realm;
  Integer port;
}
