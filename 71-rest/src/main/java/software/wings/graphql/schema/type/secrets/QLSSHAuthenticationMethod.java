package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLKerberosAuthenticationInputKeys")
public class QLSSHAuthenticationMethod implements QLObject {
  QLSSHCredentialType sshCredentialType;
  QLInlineSSHKey inlineSSHKey;
  QLSSHKeyFile sshKeyFile;
  QLSSHPassword serverPassword;
}
