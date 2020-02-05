package software.wings.graphql.datafetcher.secrets;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

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
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

@Slf4j
public class CreateSecretDataFetcher extends BaseMutatorDataFetcher<QLCreateSecretInput, QLCreateSecretPayload> {
  @Inject private SecretManager secretManager;
  @Inject private SecretController secretController;
  @Inject private SettingsService settingsService;
  @Inject private WinRMCredentialController winRMCredentialController;
  @Inject private EncryptedTextController encryptedTextController;
  @Inject
  public CreateSecretDataFetcher(SecretManager secretManager, SecretController secretController) {
    super(QLCreateSecretInput.class, QLCreateSecretPayload.class);
    this.secretManager = secretManager;
    this.secretController = secretController;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCreateSecretPayload mutateAndFetch(QLCreateSecretInput input, MutationContext mutationContext) {
    QLSecret secret = null;
    if (input.getSecretType() == QLSecretType.ENCRYPTED_TEXT) {
      String secretId = secretController.createEncryptedText(input, mutationContext.getAccountId());
      EncryptedData encryptedText = secretManager.getSecretById(mutationContext.getAccountId(), secretId);
      secret = encryptedTextController.populateEncryptedText(encryptedText);
    } else if (input.getSecretType() == QLSecretType.WINRM_CREDENTIAL) {
      SettingAttribute settingAttribute = secretController.createSettingAttribute(input.getWinRMCredential());
      SettingAttribute savedSettingAttribute =
          settingsService.saveWithPruning(settingAttribute, GLOBAL_APP_ID, mutationContext.getAccountId());
      secret = winRMCredentialController.populateWinRMCredential(savedSettingAttribute);
    }
    return QLCreateSecretPayload.builder().clientMutationId(mutationContext.getAccountId()).secret(secret).build();
  }
}
