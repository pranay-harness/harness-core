package io.harness.delegate.task.azure;

import io.harness.azure.model.AzureConfig;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.azure.AzureConfigDTO;
import io.harness.delegate.beans.azure.AzureVMAuthDTO;
import io.harness.delegate.beans.azure.registry.AzureRegistry;
import io.harness.delegate.beans.azure.registry.AzureRegistryFactory;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotSetupParameters;
import io.harness.delegate.task.azure.request.AzureVMSSSetupTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
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
        .azureEnvironmentType(azureConfigDTO.getAzureEnvironmentType())
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
  }

  private void decryptAzureWebAppSlotSetupParameters(AzureWebAppSlotSetupParameters azureAppServiceTaskParameters) {
    AzureRegistryType azureRegistryType = azureAppServiceTaskParameters.getAzureRegistryType();
    AzureRegistry azureRegistry = AzureRegistryFactory.getAzureRegistry(azureRegistryType);
    Optional<DecryptableEntity> authCredentialsDTO =
        azureRegistry.getAuthCredentialsDTO(azureAppServiceTaskParameters.getConnectorConfigDTO());
    authCredentialsDTO.ifPresent(decryptedEntity
        -> secretDecryptionService.decrypt(decryptedEntity, azureAppServiceTaskParameters.getEncryptedDataDetails()));
  }
}
