package software.wings.delegatetasks.azure;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceDockerSetting;
import io.harness.azure.model.AzureConfig;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.azure.AzureConfigDTO;
import io.harness.delegate.beans.azure.AzureVMAuthDTO;
import io.harness.delegate.beans.azure.registry.AzureRegistry;
import io.harness.delegate.beans.azure.registry.AzureRegistryFactory;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.task.azure.AzureTaskResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppRollbackParameters;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotSetupParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import io.harness.delegate.task.azure.request.AzureVMSSSetupTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
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
        == azureAppServiceTaskParameters.getCommandType()) {
      decryptAzureWebAppSlotSetupParameters((AzureWebAppSlotSetupParameters) azureAppServiceTaskParameters);
    }

    if (AzureAppServiceTaskParameters.AzureAppServiceTaskType.SLOT_ROLLBACK
        == azureAppServiceTaskParameters.getCommandType()) {
      decryptAzureWebAppRollbackParameters((AzureWebAppRollbackParameters) azureAppServiceTaskParameters);
    }
  }

  private void decryptAzureWebAppSlotSetupParameters(AzureWebAppSlotSetupParameters azureAppServiceTaskParameters) {
    AzureRegistryType azureRegistryType = azureAppServiceTaskParameters.getAzureRegistryType();
    AzureRegistry azureRegistry = AzureRegistryFactory.getAzureRegistry(azureRegistryType);
    Optional<DecryptableEntity> authCredentialsDTO =
        azureRegistry.getAuthCredentialsDTO(azureAppServiceTaskParameters.getConnectorConfigDTO());
    authCredentialsDTO.ifPresent(decryptedEntity
        -> secretDecryptionService.decrypt(decryptedEntity, azureAppServiceTaskParameters.getEncryptedDataDetails()));
    azureAppServiceTaskParameters.setAppSettings(decryptAppSettings(azureAppServiceTaskParameters.getAppSettings()));
    azureAppServiceTaskParameters.setConnSettings(decryptConnSettings(azureAppServiceTaskParameters.getConnSettings()));
  }

  private void decryptAzureWebAppRollbackParameters(AzureWebAppRollbackParameters azureAppServiceTaskParameters) {
    AzureAppServicePreDeploymentData preDeploymentData = azureAppServiceTaskParameters.getPreDeploymentData();
    preDeploymentData.setAppSettingsToAdd(decryptAppSettings(preDeploymentData.getAppSettingsToAdd()));
    preDeploymentData.setAppSettingsToRemove(decryptAppSettings(preDeploymentData.getAppSettingsToRemove()));
    preDeploymentData.setConnSettingsToAdd(decryptConnSettings(preDeploymentData.getConnSettingsToAdd()));
    preDeploymentData.setConnSettingsToRemove(decryptConnSettings(preDeploymentData.getConnSettingsToRemove()));
  }

  public Map<String, AzureAppServiceApplicationSetting> decryptAppSettings(
      Map<String, AzureAppServiceApplicationSetting> appSettings) {
    return appSettings;
  }

  public Map<String, AzureAppServiceConnectionString> decryptConnSettings(
      Map<String, AzureAppServiceConnectionString> connSettings) {
    return connSettings;
  }

  public void encryptAzureTaskResponseParams(AzureTaskResponse azureTaskResponse) {
    if (azureTaskResponse instanceof AzureWebAppSlotSetupResponse) {
      encryptAzureWebAppSlotSetupResponseParams((AzureWebAppSlotSetupResponse) azureTaskResponse);
    }
  }

  private void encryptAzureWebAppSlotSetupResponseParams(AzureWebAppSlotSetupResponse azureTaskResponse) {
    AzureAppServicePreDeploymentData preDeploymentData = azureTaskResponse.getPreDeploymentData();
    preDeploymentData.setAppSettingsToRemove(encryptAppSettings(preDeploymentData.getAppSettingsToRemove()));
    preDeploymentData.setAppSettingsToAdd(encryptAppSettings(preDeploymentData.getAppSettingsToAdd()));
    preDeploymentData.setConnSettingsToRemove(encryptConnSettings(preDeploymentData.getConnSettingsToRemove()));
    preDeploymentData.setConnSettingsToAdd(encryptConnSettings(preDeploymentData.getConnSettingsToAdd()));
    preDeploymentData.setDockerSettingsToAdd(encryptDockerSettings(preDeploymentData.getDockerSettingsToAdd()));
  }

  public Map<String, AzureAppServiceApplicationSetting> encryptAppSettings(
      Map<String, AzureAppServiceApplicationSetting> appSettings) {
    return appSettings;
  }

  public Map<String, AzureAppServiceConnectionString> encryptConnSettings(
      Map<String, AzureAppServiceConnectionString> connSettings) {
    return connSettings;
  }

  public Map<String, AzureAppServiceDockerSetting> encryptDockerSettings(
      Map<String, AzureAppServiceDockerSetting> dockerSettings) {
    return dockerSettings;
  }
}
