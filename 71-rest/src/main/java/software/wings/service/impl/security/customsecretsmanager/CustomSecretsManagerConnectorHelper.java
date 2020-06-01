package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.exception.WingsException.USER;
import static software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType.BASH;
import static software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType.POWERSHELL;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingValue.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import io.harness.security.encryption.EncryptedDataParams;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig.CustomSecretsManagerConfigKeys;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType;
import software.wings.service.intfc.SettingsService;

import java.util.Optional;
import java.util.Set;

class CustomSecretsManagerConnectorHelper {
  private SettingsService settingsService;

  @Inject
  CustomSecretsManagerConnectorHelper(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  void setConnectorInConfig(
      CustomSecretsManagerConfig customSecretsManagerConfig, Set<EncryptedDataParams> testVariables) {
    String accountId = customSecretsManagerConfig.getAccountId();
    String connectorId = customSecretsManagerConfig.getConnectorId();
    ScriptType scriptType = customSecretsManagerConfig.getCustomSecretsManagerShellScript().getScriptType();

    if (customSecretsManagerConfig.isConnectorTemplatized()) {
      connectorId = getConnectorIdFromTestVariables(testVariables);
    }
    EncryptableSetting remoteHostConnector = getConnector(accountId, connectorId, scriptType);
    customSecretsManagerConfig.setRemoteHostConnector(remoteHostConnector);
  }

  private EncryptableSetting getConnector(
      @NotEmpty String accountId, @NotEmpty String connectorId, @NotEmpty ScriptType scriptType) {
    EncryptableSetting encryptableSetting =
        (EncryptableSetting) Optional.ofNullable(settingsService.getSettingValueById(accountId, connectorId))
            .<InvalidArgumentsException>orElseThrow(() -> {
              String errorMessage = String.format("Connector with id %s was not found", connectorId);
              throw new InvalidArgumentsException(errorMessage, USER);
            });
    if (!((scriptType == BASH && encryptableSetting.getSettingType() == HOST_CONNECTION_ATTRIBUTES)
            || (scriptType == POWERSHELL && encryptableSetting.getSettingType() == WINRM_CONNECTION_ATTRIBUTES))) {
      String errorMessage =
          "Bash Script can only be used with SSH Connector and Powershell Script can only be used with WinRM Connector";
      throw new InvalidArgumentsException(errorMessage, USER);
    }
    return encryptableSetting;
  }

  private static String getConnectorIdFromTestVariables(Set<EncryptedDataParams> testVariables) {
    return testVariables.stream()
        .filter(secretVariable -> secretVariable.getName().equals(CustomSecretsManagerConfigKeys.connectorId))
        .findFirst()
        .<InvalidArgumentsException>orElseThrow(() -> {
          String errorMessage = "There is no connector supplied as a variable although the connector was templatized";
          throw new InvalidArgumentsException(errorMessage, USER);
        })
        .getValue();
  }
}
