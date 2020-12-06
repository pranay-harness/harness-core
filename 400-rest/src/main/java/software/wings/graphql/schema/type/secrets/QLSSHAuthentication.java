package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSHAuthenticationKeys")
public class QLSSHAuthentication implements QLSSHAuthenticationType {
  String userName;
  Integer port;
}
