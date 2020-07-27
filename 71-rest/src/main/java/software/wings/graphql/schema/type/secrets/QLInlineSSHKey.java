package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLInlineSSHKeyKeys")
public class QLInlineSSHKey implements QLObject {
  String sshKeySecretFileId;
  String passphraseSecretId;
}
