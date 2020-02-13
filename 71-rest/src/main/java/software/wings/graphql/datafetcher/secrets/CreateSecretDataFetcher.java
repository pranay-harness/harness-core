package software.wings.graphql.datafetcher.secrets;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.secrets.input.QLCreateSecretInput;
import software.wings.graphql.schema.mutation.secrets.payload.QLCreateSecretPayload;
import software.wings.graphql.schema.type.secrets.QLSecret;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.SecretManager;

@Slf4j
public class CreateSecretDataFetcher extends BaseMutatorDataFetcher<QLCreateSecretInput, QLCreateSecretPayload> {
  @Inject private SecretManager secretManager;
  @Inject private WinRMCredentialController winRMCredentialController;
  @Inject private EncryptedTextController encryptedTextController;
  @Inject private SSHCredentialController sshCredentialController;
  @Inject
  public CreateSecretDataFetcher(SecretManager secretManager) {
    super(QLCreateSecretInput.class, QLCreateSecretPayload.class);
    this.secretManager = secretManager;
  }

  private SettingAttribute saveSSHCredential(QLCreateSecretInput input, String accountId) {
    if (input.getSshCredential() == null) {
      throw new InvalidRequestException(
          String.format("No ssh credential input provided with the request with secretType %s", input.getSecretType()));
    }
    return sshCredentialController.createSettingAttribute(input.getSshCredential(), accountId);
  }

  private SettingAttribute saveWinRMCredential(QLCreateSecretInput input, String accountId) {
    if (input.getWinRMCredential() == null) {
      throw new InvalidRequestException(String.format(
          "No winRM credential input provided with the request with secretType %s", input.getSecretType()));
    }
    return winRMCredentialController.createSettingAttribute(input.getWinRMCredential(), accountId);
  }

  private EncryptedData saveEncryptedText(QLCreateSecretInput input, String accountId) {
    if (input.getEncryptedText() == null) {
      throw new InvalidRequestException(
          String.format("No encrypted text input provided with the request with secretType %s", input.getSecretType()));
    }
    String secretId = encryptedTextController.createEncryptedText(input, accountId);
    return secretManager.getSecretById(accountId, secretId);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCreateSecretPayload mutateAndFetch(QLCreateSecretInput input, MutationContext mutationContext) {
    QLSecret secret = null;
    if (input.getSecretType() == QLSecretType.ENCRYPTED_TEXT) {
      EncryptedData encryptedText = saveEncryptedText(input, mutationContext.getAccountId());
      secret = encryptedTextController.populateEncryptedText(encryptedText);
    } else if (input.getSecretType() == QLSecretType.WINRM_CREDENTIAL) {
      SettingAttribute savedSettingAttribute = saveWinRMCredential(input, mutationContext.getAccountId());
      secret = winRMCredentialController.populateWinRMCredential(savedSettingAttribute);
    } else if (input.getSecretType() == QLSecretType.SSH_CREDENTIAL) {
      SettingAttribute savedSettingAttribute = saveSSHCredential(input, mutationContext.getAccountId());
      secret = sshCredentialController.populateSSHCredential(savedSettingAttribute);
    }
    return QLCreateSecretPayload.builder().clientMutationId(mutationContext.getAccountId()).secret(secret).build();
  }
}
