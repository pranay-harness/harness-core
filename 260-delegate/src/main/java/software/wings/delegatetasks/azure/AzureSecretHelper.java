package software.wings.delegatetasks.azure;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.azure.model.AzureConfig;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.azure.AzureConfigDTO;
import io.harness.delegate.beans.azure.AzureVMAuthDTO;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingDTO;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceAzureSettingValue;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceHarnessSettingSecretRef;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceHarnessSettingSecretValue;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValue;
import io.harness.delegate.beans.azure.registry.AzureRegistry;
import io.harness.delegate.beans.azure.registry.AzureRegistryFactory;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.task.azure.AzureTaskResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppRollbackParameters;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotSetupParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import io.harness.delegate.task.azure.request.AzureVMSSSetupTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.encryptors.clients.LocalEncryptor;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureSecretHelper {
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private LocalEncryptor localEncryptor;

  public AzureConfig decryptAndGetAzureConfig(
      AzureConfigDTO azureConfigDTO, List<EncryptedDataDetail> azureConfigEncryptionDetails) {
    secretDecryptionService.decrypt(azureConfigDTO, azureConfigEncryptionDetails);
    return AzureConfig.builder()
        .clientId(azureConfigDTO.getClientId())
        .tenantId(azureConfigDTO.getTenantId())
        .key(azureConfigDTO.getKey().getDecryptedValue())
        .build();
  }

  public void decryptAzureVMSSTaskParameters(AzureVMSSTaskParameters azureVMSSTaskParameters) {
    if (AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_SETUP == azureVMSSTaskParameters.getCommandType()) {
      AzureVMSSSetupTaskParameters setupTaskParameters = (AzureVMSSSetupTaskParameters) azureVMSSTaskParameters;
      AzureVMAuthDTO azureVmAuthDTO = setupTaskParameters.getAzureVmAuthDTO();
      List<EncryptedDataDetail> vmAuthDTOEncryptionDetails = setupTaskParameters.getVmAuthDTOEncryptionDetails();
      secretDecryptionService.decrypt(azureVmAuthDTO, vmAuthDTOEncryptionDetails);
    }
  }

  public void decryptAzureAppServiceTaskParameters(AzureAppServiceTaskParameters azureAppServiceTaskParameters) {
    if (AzureAppServiceTaskParameters.AzureAppServiceTaskType.SLOT_SETUP
            == azureAppServiceTaskParameters.getCommandType()
        && azureAppServiceTaskParameters instanceof AzureWebAppSlotSetupParameters) {
      decryptAzureWebAppSlotSetupParameters((AzureWebAppSlotSetupParameters) azureAppServiceTaskParameters);
    }

    if (AzureAppServiceTaskParameters.AzureAppServiceTaskType.SLOT_ROLLBACK
            == azureAppServiceTaskParameters.getCommandType()
        && azureAppServiceTaskParameters instanceof AzureWebAppRollbackParameters) {
      decryptAzureWebAppRollbackParameters((AzureWebAppRollbackParameters) azureAppServiceTaskParameters);
    }
  }

  private void decryptAzureWebAppSlotSetupParameters(AzureWebAppSlotSetupParameters azureAppServiceTaskParameters) {
    List<EncryptedDataDetail> encryptedDataDetails = azureAppServiceTaskParameters.getEncryptedDataDetails();
    AzureRegistryType azureRegistryType = azureAppServiceTaskParameters.getAzureRegistryType();
    AzureRegistry azureRegistry = AzureRegistryFactory.getAzureRegistry(azureRegistryType);
    ConnectorConfigDTO connectorConfigDTO = azureAppServiceTaskParameters.getConnectorConfigDTO();
    Optional<DecryptableEntity> authCredentialsDTO = azureRegistry.getAuthCredentialsDTO(connectorConfigDTO);
    authCredentialsDTO.ifPresent(credentials -> secretDecryptionService.decrypt(credentials, encryptedDataDetails));

    decryptSettings(azureAppServiceTaskParameters.getAppSettings());
    decryptSettings(azureAppServiceTaskParameters.getConnSettings());
  }

  private void decryptAzureWebAppRollbackParameters(AzureWebAppRollbackParameters azureWebAppRollbackParameters) {
    AzureAppServicePreDeploymentData preDeploymentData = azureWebAppRollbackParameters.getPreDeploymentData();
    decryptSettings(preDeploymentData.getAppSettingsToAdd());
    decryptSettings(preDeploymentData.getAppSettingsToRemove());
    decryptSettings(preDeploymentData.getConnStringsToAdd());
    decryptSettings(preDeploymentData.getConnStringsToRemove());
    decryptSettings(preDeploymentData.getDockerSettingsToAdd());
  }

  private <T extends AzureAppServiceSettingDTO> void decryptSettings(Map<String, T> settings) {
    settings.values().forEach(
        appServiceSetting -> decryptSettingValue(appServiceSetting.getName(), appServiceSetting.getValue()));
  }

  private void decryptSettingValue(String settingName, AzureAppServiceSettingValue settingValue) {
    log.info("Checking for decryption setting: {}", settingName);
    if (settingValue instanceof AzureAppServiceHarnessSettingSecretValue) {
      decryptEncryptedRecordWithEncryptedDataDetails(
          settingName, (AzureAppServiceHarnessSettingSecretValue) settingValue);
    }

    if (settingValue instanceof AzureAppServiceAzureSettingValue) {
      decryptEncryptedRecordByLocalEncryptor(settingName, (AzureAppServiceAzureSettingValue) settingValue);
    }
  }

  private void decryptEncryptedRecordWithEncryptedDataDetails(
      String settingName, AzureAppServiceHarnessSettingSecretValue userSettingSecret) {
    log.info("Decrypting setting by encrypted details : {}", settingName);
    List<EncryptedDataDetail> encryptedDataDetails = userSettingSecret.getEncryptedDataDetails();
    AzureAppServiceHarnessSettingSecretRef settingSecretRef = userSettingSecret.getSettingSecretRef();
    secretDecryptionService.decrypt(settingSecretRef, encryptedDataDetails);
  }

  private void decryptEncryptedRecordByLocalEncryptor(
      String settingName, AzureAppServiceAzureSettingValue configurationSetting) {
    log.info("Decrypting setting by local encryptor : {}", settingName);
    String accountId = configurationSetting.getAccountId();
    EncryptedRecord encryptedRecord = configurationSetting.getEncryptedRecord();

    char[] secretValue = localEncryptor.fetchSecretValue(accountId, encryptedRecord, null);
    configurationSetting.setDecryptedValue(new String(secretValue));
  }

  public void encryptAzureTaskResponseParams(AzureTaskResponse azureTaskResponse, final String accountId) {
    if (azureTaskResponse instanceof AzureWebAppSlotSetupResponse) {
      encryptAzureWebAppSlotSetupResponseParams((AzureWebAppSlotSetupResponse) azureTaskResponse, accountId);
    }
  }

  private void encryptAzureWebAppSlotSetupResponseParams(
      AzureWebAppSlotSetupResponse azureTaskResponse, final String accountId) {
    AzureAppServicePreDeploymentData preDeploymentData = azureTaskResponse.getPreDeploymentData();
    encryptSettings(preDeploymentData.getAppSettingsToRemove(), accountId);
    encryptSettings(preDeploymentData.getAppSettingsToAdd(), accountId);
    encryptSettings(preDeploymentData.getConnStringsToRemove(), accountId);
    encryptSettings(preDeploymentData.getConnStringsToAdd(), accountId);
    encryptSettings(preDeploymentData.getDockerSettingsToAdd(), accountId);
  }

  private <T extends AzureAppServiceSettingDTO> void encryptSettings(Map<String, T> settings, final String accountId) {
    settings.values().forEach(appServiceSetting -> {
      log.info("Checking for encryption Azure setting: {}", appServiceSetting.getName());
      AzureAppServiceSettingValue value = appServiceSetting.getValue();
      if (value instanceof AzureAppServiceAzureSettingValue) {
        log.info("Encrypting Azure setting: {}", appServiceSetting.getName());
        encryptRecordByLocalEncryptor((AzureAppServiceAzureSettingValue) value, accountId);
      } else {
        throw new InvalidRequestException(
            format("Unsupported encryption on delegate for Azure App Service setting value type: [%s]",
                value != null ? value.getClass() : null));
      }
    });
  }

  private void encryptRecordByLocalEncryptor(
      AzureAppServiceAzureSettingValue configurationSetting, final String accountId) {
    String value = configurationSetting.getDecryptedValue();

    EncryptedRecord encryptedRecord = localEncryptor.encryptSecret(accountId, value, null);
    configurationSetting.setEncryptedRecord(encryptedRecord);
    configurationSetting.setDecryptedValue(EMPTY);
    configurationSetting.setAccountId(accountId);
  }
}
