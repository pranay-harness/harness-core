package software.wings.graphql.datafetcher.connector.types;

import io.harness.exception.InvalidRequestException;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.schema.mutation.connector.input.QLConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateConnectorInput;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

public abstract class Connector {
  public abstract SettingAttribute getSettingAttribute(QLConnectorInput input, String accountId);

  public abstract void updateSettingAttribute(SettingAttribute settingAttribute, QLUpdateConnectorInput input);

  public abstract void checkSecrets(QLConnectorInput input, String accountId);

  public abstract void checkSecrets(QLUpdateConnectorInput input, SettingAttribute settingAttribute);

  public abstract void checkInputExists(QLConnectorInput input);

  public abstract void checkInputExists(QLUpdateConnectorInput input);

  protected void checkSecretExists(SecretManager secretManager, String accountId, String secretId) {
    if (secretManager.getSecretById(accountId, secretId) == null) {
      throw new InvalidRequestException("Secret does not exist");
    }
  }

  protected void checkSSHSettingExists(SettingsService settingsService, String accountId, String secretId) {
    if (settingsService.getByAccount(accountId, secretId) == null) {
      throw new InvalidRequestException("Secret does not exist");
    }
  }
}
