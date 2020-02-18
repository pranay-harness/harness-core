package software.wings.graphql.datafetcher.secrets;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.SettingAttribute.SettingCategory.SETTING;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingValue.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.secrets.input.QLDeleteSecretInput;
import software.wings.graphql.schema.mutation.secrets.payload.QLDeleteSecretPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;

@Slf4j
public class DeleteSecretDataFetcher extends BaseMutatorDataFetcher<QLDeleteSecretInput, QLDeleteSecretPayload> {
  @Inject private SecretManager secretManager;
  @Inject private SettingsService settingsService;

  @Inject
  public DeleteSecretDataFetcher() {
    super(QLDeleteSecretInput.class, QLDeleteSecretPayload.class);
  }

  private void throwInvalidSecretException(String secretId) {
    throw new InvalidRequestException(String.format("No secret exists with the id %s", secretId));
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLDeleteSecretPayload mutateAndFetch(QLDeleteSecretInput input, MutationContext mutationContext) {
    String secretId = input.getSecretId();
    String accountId = mutationContext.getAccountId();
    if (isBlank(secretId)) {
      throw new InvalidRequestException("The secretId cannot be null in the delete request");
    }
    EncryptedData encryptedData = secretManager.getSecretById(accountId, secretId);
    if (encryptedData != null) {
      // The secret is either encrypted Text or encrypted File, also ensuring that no other encrypted record is deleted
      if (encryptedData.getType() == SettingValue.SettingVariableTypes.SECRET_TEXT) {
        secretManager.deleteSecret(accountId, secretId);
      } else {
        throwInvalidSecretException(secretId);
      }
    } else {
      // Check whether the secret is winRM/SSH
      SettingAttribute settingAttribute = settingsService.get(secretId);
      // Since we did get without accountId, adding a check for accountId here.
      if (settingAttribute == null || !settingAttribute.getAccountId().equals(accountId)
          || settingAttribute.getCategory() != SETTING) {
        throwInvalidSecretException(secretId);
      }
      SettingValue settingValue = null;
      if (settingAttribute != null) {
        settingValue = settingAttribute.getValue();
      }
      if (settingValue != null
          && (settingValue.getSettingType() == HOST_CONNECTION_ATTRIBUTES
                 || settingValue.getSettingType() == WINRM_CONNECTION_ATTRIBUTES)) {
        settingsService.delete(null, secretId);
      } else {
        throwInvalidSecretException(secretId);
      }
    }
    return QLDeleteSecretPayload.builder().clientMutationId(input.getClientMutationId()).build();
  }
}
