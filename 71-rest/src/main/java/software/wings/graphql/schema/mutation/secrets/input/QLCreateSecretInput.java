package software.wings.graphql.schema.mutation.secrets.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.secrets.QLEncryptedTextInput;
import software.wings.graphql.schema.type.secrets.QLSSHCredentialInput;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.graphql.schema.type.secrets.QLWinRMCredentialInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateEncryptedTextInputKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLCreateSecretInput implements QLMutationInput {
  String clientMutationId;
  QLSecretType secretType;
  QLEncryptedTextInput encryptedText;
  QLWinRMCredentialInput winRMCredential;
  QLSSHCredentialInput sshCredential;
}
