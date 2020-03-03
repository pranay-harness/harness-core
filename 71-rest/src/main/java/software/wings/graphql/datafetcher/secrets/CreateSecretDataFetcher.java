package software.wings.graphql.datafetcher.secrets;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.secrets.input.QLCreateSecretInput;
import software.wings.graphql.schema.mutation.secrets.payload.QLCreateSecretPayload;
import software.wings.graphql.schema.type.secrets.QLSecret;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

@Slf4j
public class CreateSecretDataFetcher extends BaseMutatorDataFetcher<QLCreateSecretInput, QLCreateSecretPayload> {
  @Inject private SecretManager secretManager;
  @Inject private WinRMCredentialController winRMCredentialController;
  @Inject private EncryptedTextController encryptedTextController;
  @Inject private SSHCredentialController sshCredentialController;
  @Inject private SettingsService settingsService;
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
    SettingAttribute settingAttribute =
        sshCredentialController.createSettingAttribute(input.getSshCredential(), accountId);
    return settingsService.saveWithPruning(settingAttribute, GLOBAL_APP_ID, accountId);
  }

  private SettingAttribute saveWinRMCredential(QLCreateSecretInput input, String accountId) {
    if (input.getWinRMCredential() == null) {
      throw new InvalidRequestException(String.format(
          "No winRM credential input provided with the request with secretType %s", input.getSecretType()));
    }
    SettingAttribute settingAttribute =
        winRMCredentialController.createSettingAttribute(input.getWinRMCredential(), accountId);
    return settingsService.saveWithPruning(settingAttribute, GLOBAL_APP_ID, accountId);
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
    switch (input.getSecretType()) {
      case ENCRYPTED_TEXT:
        EncryptedData encryptedText = saveEncryptedText(input, mutationContext.getAccountId());
        secret = encryptedTextController.populateEncryptedText(encryptedText);
        break;
      case WINRM_CREDENTIAL:
        SettingAttribute savedSettingAttribute = saveWinRMCredential(input, mutationContext.getAccountId());
        secret = winRMCredentialController.populateWinRMCredential(savedSettingAttribute);
        break;
      case SSH_CREDENTIAL:
        SettingAttribute savedSSH = saveSSHCredential(input, mutationContext.getAccountId());
        secret = sshCredentialController.populateSSHCredential(savedSSH);
        break;
      case ENCRYPTED_FILE:
        throw new InvalidRequestException("Encrypted file secret cannot be created through API.");
      default:
        throw new InvalidRequestException("Invalid secret Type");
    }
    return QLCreateSecretPayload.builder().clientMutationId(mutationContext.getAccountId()).secret(secret).build();
  }
}
