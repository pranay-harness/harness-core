package software.wings.delegatetasks.azure;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.azure.AzureConfigDTO;
import io.harness.delegate.beans.azure.AzureVMAuthDTO;
import io.harness.delegate.task.azure.request.AzureVMSSSetupTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
}
