package software.wings.graphql.schema.type.secrets;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSHAuthenticationInputKeys")
public class QLSSHAuthenticationInput implements QLObject {
  String userName;
  Integer port;
  QLSSHAuthenticationMethod sshAuthenticationMethod;
}
