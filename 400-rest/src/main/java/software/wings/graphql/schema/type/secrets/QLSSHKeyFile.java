package software.wings.graphql.schema.type.secrets;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSHCredentialInputKeys")
public class QLSSHKeyFile implements QLObject {
  String path;
  String passphraseSecretId;
}
