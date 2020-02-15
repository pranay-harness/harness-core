package software.wings.graphql.schema.mutation.secrets.input;

import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.secrets.QLEncryptedTextUpdate;
import software.wings.graphql.schema.type.secrets.QLSSHCredentialUpdate;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.graphql.schema.type.secrets.QLWinRMCredentialUpdate;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateEncryptedTextInputKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLUpdateSecretInput implements QLMutationInput {
  String clientMutationId;
  String id;
  QLSecretType secretType;
  RequestField<QLEncryptedTextUpdate> encryptedText;
  RequestField<QLWinRMCredentialUpdate> winRMCredential;
  RequestField<QLSSHCredentialUpdate> sshCredential;
}
