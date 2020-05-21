package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.exception.WingsException.USER;
import static software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType.BASH;
import static software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType.POWERSHELL;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingValue.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType;
import software.wings.service.intfc.SettingsService;

import java.util.Optional;

class CustomSecretsManagerConnectorHelper {
  private SettingsService settingsService;

  @Inject
  CustomSecretsManagerConnectorHelper(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  EncryptableSetting getConnector(
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
}
