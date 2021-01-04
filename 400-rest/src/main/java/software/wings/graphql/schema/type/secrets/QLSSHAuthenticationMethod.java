package software.wings.graphql.schema.type.secrets;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLKerberosAuthenticationInputKeys")
public class QLSSHAuthenticationMethod implements QLObject {
  QLSSHCredentialType sshCredentialType;
  QLInlineSSHKey inlineSSHKey;
  QLSSHKeyFile sshKeyFile;
  QLSSHPassword serverPassword;
}
