package software.wings.graphql.datafetcher.secrets;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.secrets.input.QLUpdateSecretInput;
import software.wings.graphql.schema.mutation.secrets.payload.QLUpdateSecretPayload;
import software.wings.graphql.schema.type.secrets.QLEncryptedTextUpdate;
import software.wings.graphql.schema.type.secrets.QLSSHCredentialUpdate;
import software.wings.graphql.schema.type.secrets.QLSecret;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.graphql.schema.type.secrets.QLWinRMCredentialUpdate;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.SecretManager;

@Slf4j
public class UpdateSecretDataFetcher extends BaseMutatorDataFetcher<QLUpdateSecretInput, QLUpdateSecretPayload> {
  @Inject private SecretManager secretManager;
  @Inject private WinRMCredentialController winRMCredentialController;
  @Inject private EncryptedTextController encryptedTextController;
  @Inject private SSHCredentialController sshCredentialController;
  @Inject
  public UpdateSecretDataFetcher() {
    super(QLUpdateSecretInput.class, QLUpdateSecretPayload.class);
  }

  private SettingAttribute updateSSHCredentials(QLUpdateSecretInput updateSecretInput, String accountId) {
    QLSSHCredentialUpdate sshCredential = updateSecretInput.getSshCredential().getValue().orElse(null);
    if (sshCredential == null) {
      throw new InvalidRequestException(String.format(
          "No ssh credential input provided with the request with secretType %s", updateSecretInput.getSecretType()));
    }
    return sshCredentialController.updateSSHCredential(sshCredential, updateSecretInput.getId(), accountId);
  }

  private SettingAttribute updateWinRMCredential(QLUpdateSecretInput updateSecretInput, String accountId) {
    QLWinRMCredentialUpdate encryptedTextUpdate = updateSecretInput.getWinRMCredential().getValue().orElse(null);
    if (encryptedTextUpdate == null) {
      throw new InvalidRequestException(String.format(
          "No winRM credential input provided with the request with secretType %s", updateSecretInput.getSecretType()));
    }
    return winRMCredentialController.updateWinRMCredential(encryptedTextUpdate, updateSecretInput.getId(), accountId);
  }

  private EncryptedData updateEncryptedText(QLUpdateSecretInput updateSecretInput, String accountId) {
    QLEncryptedTextUpdate encryptedTextUpdate = updateSecretInput.getEncryptedText().getValue().orElse(null);
    if (encryptedTextUpdate == null) {
      throw new InvalidRequestException(String.format(
          "No encrypted text input provided with the request with secretType %s", updateSecretInput.getSecretType()));
    }
    encryptedTextController.updateEncryptedText(encryptedTextUpdate, updateSecretInput.getId(), accountId);
    return secretManager.getSecretById(accountId, updateSecretInput.getId());
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLUpdateSecretPayload mutateAndFetch(
      QLUpdateSecretInput updateSecretInput, MutationContext mutationContext) {
    QLSecret secret = null;
    if (updateSecretInput.getSecretType() == QLSecretType.ENCRYPTED_TEXT) {
      EncryptedData encryptedText = updateEncryptedText(updateSecretInput, mutationContext.getAccountId());
      secret = encryptedTextController.populateEncryptedText(encryptedText);
    } else if (updateSecretInput.getSecretType() == QLSecretType.WINRM_CREDENTIAL) {
      SettingAttribute updatedSettingAttribute =
          updateWinRMCredential(updateSecretInput, mutationContext.getAccountId());
      secret = winRMCredentialController.populateWinRMCredential(updatedSettingAttribute);
    } else if (updateSecretInput.getSecretType() == QLSecretType.SSH_CREDENTIAL) {
      SettingAttribute updatedSettingAttribute =
          updateSSHCredentials(updateSecretInput, mutationContext.getAccountId());
      secret = sshCredentialController.populateSSHCredential(updatedSettingAttribute);
    }
    return QLUpdateSecretPayload.builder().clientMutationId(mutationContext.getAccountId()).secret(secret).build();
  }
}
